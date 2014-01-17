package edu.berkeley.path.beats.test.simulator

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import scala.collection.JavaConversions._
import edu.berkeley.path.beats.simulator._
import edu.berkeley.path.beats.control.predictive.{RampMeteringControl, RampMeteringControlSet, AdjointRampMeteringPolicyMaker, ScenarioConverter}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.apache.log4j.Logger
import scala.collection.immutable.TreeMap
import edu.berkeley.path.ramp_metering.AdjointRampMetering
import edu.berkeley.path.beats.test.simulator.output.TestPolicyMaker

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/24/13
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(classOf[JUnitRunner])
class TestPolicyMakerTest extends FunSuite with ShouldMatchers {
  val logger = Logger.getLogger(classOf[TestPolicyMakerTest])
  test("test mpc network hand-crafted") {
    val scenario = ObjectFactory.createAndLoadScenario("src/test/resources/mpc.xml")
    scenario.initialize(1, 0, 20, 1, "xml", "hi", 1, 1)
    val meters = {
      val mtrs = new RampMeteringControlSet
      val net = scenario.getNetworkSet.getNetwork.get(0).asInstanceOf[Network]
      val mainline = ScenarioConverter.extractMainline(net)
      val mlNodes = mainline.map {
        _.getBegin_node
      }
      TreeMap(ScenarioConverter.extractOnramps(net).map {
        onramp => mlNodes.indexOf(onramp.getEnd_node) -> onramp
      }: _*).values.foreach {
        or => {
          val meter = new RampMeteringControl
          meter.min_rate = 0.0
          meter.max_rate = 1.0
          meter.link = or
          mtrs.control.add(meter)
        }
      }
      mtrs
    }
    val time_current = 0
    val pm_dt = 1
    val pm_horizon_steps = 80
    val (scen, onramps) = ScenarioConverter.convertScenario(scenario.getNetworkSet.getNetwork.get(0).asInstanceOf[Network], scenario.gather_current_fds(time_current), scenario.predict_demands(time_current, pm_dt, pm_horizon_steps), scenario.predict_split_ratios(time_current, pm_dt, pm_horizon_steps), scenario.gather_current_densities, meters, scenario.getSimdtinseconds)
    val nLinks = 12
    val nTimesteps = 80
    val dt = 1
    val offramps = List(4,9)
    val demands = Map(0 -> (0, 24.), 4 -> (10, 12.), 6 -> (15, 12.), 8 -> (15, 12.))
    val allDemands = {
      val ar = Array.fill(nTimesteps, nLinks)(0.0)
      demands.foreach{case (k, (a, b)) => {
        ar(a)(k) = b
      }}
      ar.map{_.toIndexedSeq}.toIndexedSeq
    }
    scen.fw.offramps should be (offramps.map{_ + 1})
    scen.simParams.numTimesteps should be (nTimesteps)
    scen.fw.nLinks should be (nLinks)
    scen.policyParams.deltaTimeSeconds should be (dt)
    scen.simParams.ic.density should be(List.fill(nLinks)(0.0))
    scen.simParams.ic.queue should be(List.fill(nLinks)(0.0))
    val allSplitRatios = IndexedSeq.fill(nTimesteps, offramps.size)(.9)
    scen.simParams.bc.splitRatios should be(allSplitRatios)
    scen.simParams.bc.demands should be(allDemands)
  }
}
