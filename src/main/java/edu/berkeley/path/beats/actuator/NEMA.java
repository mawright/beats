package edu.berkeley.path.beats.actuator;

/**
 * Created with IntelliJ IDEA.
 * User: gomes
 * Date: 3/6/14
 * Time: 5:24 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class NEMA {

    public static enum ID {NULL,_1,_2,_3,_4,_5,_6,_7,_8};

    public static ID string_to_nema(String str){
        if(str==null)
            return ID.NULL;
        if(str.isEmpty())
            return ID.NULL;
        if(!str.startsWith("_"))
            str = "_"+str;
        ID nema;
        try{
            nema = ID.valueOf(str);
        }
        catch(IllegalArgumentException  e){
            nema = ID.NULL;
        }
        return nema;
    }

    public static boolean is_compatible(ActuatorSignal.SignalPhase phaseA,ActuatorSignal.SignalPhase phaseB){
        if( !phaseA.protectd || !phaseB.protectd )
            return true;
        return is_compatible(phaseA.myNEMA, phaseB.myNEMA);
    }

    public static boolean is_compatible(ID nemaA, ID nemaB){

        if(nemaA==nemaB)
            return true;

        if( NEMA.isNULL(nemaA) || NEMA.isNULL(nemaB) )
            return true;

        switch(nemaA){
            case _1:
            case _2:
                if(nemaB==ID._5 || nemaB==ID._6)
                    return true;
                else
                    return false;
            case _3:
            case _4:
                if(nemaB==ID._7 || nemaB==ID._8 )
                    return true;
                else
                    return false;
            case _5:
            case _6:
                if(nemaB==ID._1 || nemaB==ID._2 )
                    return true;
                else
                    return false;
            case _7:
            case _8:
                if(nemaB==ID._3 || nemaB==ID._4 )
                    return true;
                else
                    return false;
            case NULL:
                break;
            default:
                break;
        }
        return false;
    }

    public static ID int_to_nema(int x){
        switch(x){
            case 1:
                return ID._1;
            case 2:
                return ID._2;
            case 3:
                return ID._3;
            case 4:
                return ID._4;
            case 5:
                return ID._5;
            case 6:
                return ID._6;
            case 7:
                return ID._7;
            case 8:
                return ID._8;
            default:
                return ID.NULL;
        }
    }

    public static int nema_to_int(ID x){
        switch(x){
            case _1:
                return 1;
            case _2:
                return 2;
            case _3:
                return 3;
            case _4:
                return 4;
            case _5:
                return 5;
            case _6:
                return 6;
            case _7:
                return 7;
            case _8:
                return 8;
            default:
                return 0;
        }    }


    public static int get_ring(ID nema){

        switch(nema){
            case _1:
            case _2:
            case _3:
            case _4:
                return 1;
            case _5:
            case _6:
            case _7:
            case _8:
                return 2;
            case NULL:
                return 0;
            default:
                return 0;
        }
    }

    public static boolean isNULL(ID nema){
        return nema==ID.NULL;
    }

    public static NEMA.ID get_opposing(NEMA.ID x){

        switch(x){
            case _1:
                return NEMA.ID._2;
            case _2:
                return NEMA.ID._1;
            case _3:
                return NEMA.ID._4;
            case _4:
                return NEMA.ID._3;
            case _5:
                return NEMA.ID._6;
            case _6:
                return NEMA.ID._5;
            case _7:
                return NEMA.ID._8;
            case _8:
                return NEMA.ID._7;
            case NULL:
                return NEMA.ID.NULL;
        }
        return NEMA.ID.NULL;
    }

    public static int get_ring_group(NEMA.ID x){
        switch(x){
            case _1:
            case _2:
            case _5:
            case _6:
                return 0;
            case _3:
            case _4:
            case _7:
            case _8:
                return 1;
            case NULL:
                return -1;
        }
        return -1;
    }

    public static boolean is_through(NEMA.ID x){
        switch(x){
            case _1:
            case _3:
            case _5:
            case _7:
                return false;
            case _2:
            case _4:
            case _6:
            case _8:
                return true;
        }
        return false;
    }
}
