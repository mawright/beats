package edu.berkeley.path.beats.control.predictive

import edu.berkeley.path.beats.simulator._
import edu.berkeley.path.ramp_metering._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import edu.berkeley.path.ramp_metering.FreewayLink
import edu.berkeley.path.ramp_metering.PolicyParameters
import scala.Some
import edu.berkeley.path.ramp_metering.FreewayScenario
import edu.berkeley.path.ramp_metering.FundamentalDiagram
import scala.collection.immutable.TreeMap
import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet
import edu.berkeley.path.ramp_metering.InitialConditions
import edu.berkeley.path.ramp_metering.PolicyParameters
import scala.Some
import edu.berkeley.path.ramp_metering.FreewayLink
import edu.berkeley.path.ramp_metering.Freeway
import edu.berkeley.path.ramp_metering.SimulationParameters
import edu.berkeley.path.ramp_metering.BoundaryConditions
import edu.berkeley.path.ramp_metering.FreewayScenario
import edu.berkeley.path.ramp_metering.FundamentalDiagram
import java.{lang, util}

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/24/13
 * Time: 12:05 PM
 * To change this template use File | Settings | File Templates.
 */

object ScenarioConverter {

  def convertScenario(net: Network,
                      fd: FundamentalDiagramSet,
                      demand: DemandSet,
                      splitRatios: SplitRatioSet,
                      ics: InitialDensitySet,
                      control: RampMeteringControlSet,
                      dt: Double) = {
    val mainline = extractMainline(net)
    val mlNodes = mainline.map {_.getBegin_node}
    val onramps = TreeMap(extractOnramps(net).map {
      onramp => mlNodes.indexOf(onramp.getEnd_node) -> onramp
    }: _*)
    val onrampNodes = onramps.map{case (i,l) => l.getBegin_node}
    val sources = extractSources(net)
    val onrampSources = sources.filter{source => onrampNodes.contains(source.getEnd_node)}
    val mainlineSource = sources.diff(onrampSources).head
    val firstPair = mainline.head -> mainlineSource
    val backPairs = onramps.values.map{case onramp => onramp -> onrampSources.filter{_.getEnd_node == onramp.getBegin_node}.head}.toList
    val onrampSourcePairs = firstPair :: backPairs.toList
    val offramps = TreeMap(extractOfframps(net).map {
      offramp => mlNodes.indexOf(offramp.getBegin_node) -> offramp
    }: _*)
    val fds = fd.getFundamentalDiagramProfile.map {
      prof => net.getLinkWithId(prof.getLinkId) -> prof.getFundamentalDiagram.toList.head
    }.toMap
    val links = mainline.zipWithIndex.map {
      case (link, i) => {
        val fd = fds(link)
        val rmax = if (i == 0) fds(onrampSourcePairs.head._2).getCapacity else { onramps.get(i) match {
          case Some(onramp) => fds(onramp).getCapacity
          case None => 0.0
        }
      }
        val p = 4.0
        FreewayLink(FundamentalDiagram(fd.getFreeFlowSpeed, fd.getCapacity, fd.getJamDensity), link.getLength, rmax, p)
      }
    }
    val freeway = Freeway(links.toIndexedSeq,  0 +: onramps.keys.toIndexedSeq, offramps.keys.toIndexedSeq)
    val policyParams = extractPolicyParameters(dt)
    // This is assumed to be evenly divisible
    assert(demand.getDemandProfile.head.getDt % dt.toInt == 0)
    val bcDtFactor = (demand.getDemandProfile.head.getDt / dt.toInt).toInt
    val indexedDemand = demand.getDemandProfile.map {
      profile => {
        net.getLinkWithId(profile.getLinkIdOrg) -> profile.getDemand.toList.head
      }
    }.toMap
   val demands = onrampSourcePairs.map {
     case (ramp, source) => {
       indexedDemand(source).getContent.split(",").map { p =>
         Array.fill(bcDtFactor)(p.toDouble)
       }
     }.flatten
   }.toIndexedSeq.transpose
    println(onrampSourcePairs.map{case(r,s) => r.getId}.mkString(","))
    println(onrampSourcePairs.map{case(r,s) => s.getId}.mkString(","))
    // val demands = onrampSourcePairs.map {
    //   case (ramp, source) => {
    //     indexedDemand(source).getContent.split(",").map{_.toDouble}.toIndexedSeq
    //   }
    // }.toIndexedSeq.transpose
    val srIndex = splitRatios.getSplitRatioProfile.toList.flatMap{_.getSplitratio.toList}.map{profile => net.getLinkWithId(profile.getLinkOut) -> profile.getContent.split(",").map{_.toDouble}}.toMap
    val splits = {
      val withoutLast = offramps.values.map {
        srIndex(_).toIndexedSeq.map { p =>
          Array.fill(bcDtFactor)(1 - p)
          }.flatten
          }.toIndexedSeq
      val lastPiece = IndexedSeq.fill(withoutLast.head.size)(1.0)
      (withoutLast :+ lastPiece).transpose
    }
    val bc = BoundaryConditions(demands, splits)
    val icLookup = ics.getDensity.toList.map {
      d => {
        net.getLinkWithId(d.getLinkId) -> d.getContent.toDouble
      }
    }.toMap
    val ic = InitialConditions(
      mainline.map {p =>
        icLookup.getOrElse(p, 0.0)
      }.toIndexedSeq,
      (0 until mainline.length).map {
        i => {
          onramps.get(i) match {
            case None => 0.0
            case Some(onramp) => icLookup(onramp)
          }
        }
      }.toIndexedSeq
    )
    val index = control.control.toList.map{s => s.link -> (s.min_rate / fds(s.link).getCapacity -> s.max_rate / fds(s.link).getCapacity)}.toMap
    val simParams = SimulationParameters(bc, ic, Some(MeterSpec(onrampSourcePairs.tail.map{case (o, s) => index(o)}.toList)))
    val scen = (FreewayScenario(freeway, simParams, policyParams), onramps.values)
    scen
  }

  def extractPolicyParameters(dt: Double) = {
    PolicyParameters(dt)
  }

  def extractMainline(net: Network) = {
    val mainlineSource = extractMainlineSource(net)
    val orderedMainlineBuffer = ListBuffer[Link](mainlineSource)
    var link = mainlineSource
    while (!link.isSink) {
      link = link.getEnd_node.getOutput_link.filter {
        _.getLinkType.getName == "Freeway"
      }.head
      orderedMainlineBuffer += link
    }
    orderedMainlineBuffer.toList
  }

  def extractOnramps(net: Network) = {
    net.getListOfLinks.toList.map {
      _.asInstanceOf[Link]
    }.filter {
      _.getLinkType.getName == "On-Ramp"
    }
  }

  def extractSources(net: Network) = {
    net.getListOfLinks.toList.map{_.asInstanceOf[Link]}.filter{_.getLinkType.getName == "Source"}
  }

  def extractOfframps(net: Network) = {
    net.getListOfLinks.toList.map {
      _.asInstanceOf[Link]
    }.filter {
      _.getLinkType.getName == "Off-Ramp"
    }
  }

  def isMainlineSource(link: Link) = {
    link.getLinkType.getName == "Freeway" && link.getBegin_node.getInput_link.toList.forall{_.getLinkType.getName != "Freeway"}
  }

  def extractMainlineSource(net: Network) = {
    net.getListOfLinks.toList.map {
      _.asInstanceOf[Link]
    }.filter {
      isMainlineSource _
    }.head
  }

  implicit def toDoubleList( lst: List[Double] ) =
    seqAsJavaList( lst.map( i => i:java.lang.Double ) )
}


import ScenarioConverter.toDoubleList


/*
The dt's across all structures are assumed to be equal to dt.
The network is assumed to have the following structure:
               /       /
     ----- ---- ------ ------>
    /                 /

where the first ramp must exist at the beginning of the network and the horizontal part must be all Freeway links.
*/

class AdjointRampMeteringPolicyMaker extends RampMeteringPolicyMaker {
  def padZeros(arr: IndexedSeq[IndexedSeq[Double]], factor: Double) = {
    arr ++ IndexedSeq.fill[IndexedSeq[Double]](math.round(arr.length * factor).toInt)(IndexedSeq.fill[Double](arr(0).length)(0.0))
  }
  def padSplits(arr: IndexedSeq[IndexedSeq[Double]], factor: Double) = {
    arr ++ IndexedSeq.fill[IndexedSeq[Double]](math.round(arr.length * factor).toInt)(arr.last)
  }
  def givePolicy(net: Network, fd: FundamentalDiagramSet, demand: DemandSet, splitRatios: SplitRatioSet, ics: InitialDensitySet, control: RampMeteringControlSet, dt: lang.Double): RampMeteringPolicySet = {
    val (scenario, onramps) = ScenarioConverter.convertScenario(net, fd, demand, splitRatios, ics, control, dt)
    var scen = scenario

    var params = scen.simParams
    val origT = params.numTimesteps
    var simstate = new BufferCtmSimulator(scen).simulate(AdjointRampMetering.noControl(scen))
    while (simstate.density.last.sum + simstate.queue.last.sum > params.bc.demands.map{_.sum}.sum * .01) {
      println(simstate.density.last.sum + ", " + simstate.queue.last.sum  + ", " + params.bc.demands.map{_.sum}.sum * .01)
      params = SimulationParameters(BoundaryConditions(padZeros(params.bc.demands, .3), padSplits(params.bc.splitRatios, .3)), params.ic)
      scen = FreewayScenario(scen.fw, params, scen.policyParams)
      simstate = new BufferCtmSimulator(scen).simulate(AdjointRampMetering.noControl(scen))
    }
    println(scen)
//    Adjoint.optimizer = new IpOptAdjointOptimizer
    Adjoint.maxIter = 20
    val output = AdjointRampMetering.controlledOutput(scen, new AdjointRampMetering(scen.fw))
    println(output)
    val flux = output.fluxRamp.take(origT).transpose
    val queues = output.queue.take(origT).transpose.map{_.map{_ / scen.policyParams.deltaTimeSeconds}}
    val rmax = scen.fw.rMaxs
    val rampPolicy = (flux, queues, rmax).zipped.map{case (f,q,r) => {
      (f,q).zipped.map{case (ff,qq) => {
        if (ff >= qq || ff >= .95 * r || qq / scen.policyParams.deltaTimeSeconds <= .01 * r) r else ff
        }}
    }
    }
    val set = new RampMeteringPolicySet
    scen.fw.onramps.tail.map{rampPolicy(_)}.zip(onramps).foreach{ case (fl, or) => {
      val limits = control.control.filter{_.link == or}.head
      val lower_limit = limits.min_rate
      val upper_limit = limits.max_rate
      val profile = new RampMeteringPolicyProfile
      profile.sensorLink = or
      profile.rampMeteringPolicy = fl.toList.map{v => math.min(upper_limit, math.max(lower_limit, v))}
      set.profiles.add(profile)
    }}
    set
  }
}