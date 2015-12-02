package edu.berkeley.path.beats.simulator.utils;

import java.io.*;
import java.util.HashMap;

/**
 * Created by gomes on 3/27/14.
 */
public class DebugLogger implements Serializable {
    private static final long serialVersionUID = -5719086319108642449L;

    private static HashMap<Integer,Writer> writers;

    public static int add_writer(String filename){
        if(writers==null)
            writers = new HashMap<Integer,Writer>();
        int id;
        try {
            id = (int)(Math.random() *100000)+1;
            writers.put(id,new FileWriter(new File(filename)));
        } catch (IOException e) {
            id = 0;
        }
        return id;
    }

    public static void write(int id,Object x){
        if(writers==null)
            return;
        try {
            writers.get(id).write(x.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close_all(){
        if(writers==null)
            return;
        try {
            for(Writer w : writers.values()){
                w.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
