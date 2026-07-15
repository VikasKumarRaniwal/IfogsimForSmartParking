package org.fog.utils;

import java.util.*;

public class O2OModel {

    // ================= TASK =================
    public static class Task {
        private static int ID = 1;
        public final int id;

        public double priority;
        public double instrMI;
        public double cpuReq;
        public double memReq;
        public double bwReq;
        public double maxLatency;
        public double dataSizeMB;

        public Task(double p, double instr, double c, double m,
                    double b, double maxLat, double data) {
            id = ID++;
            priority = p;
            instrMI = instr;
            cpuReq = c;
            memReq = m;
            bwReq = b;
            maxLatency = maxLat;
            dataSizeMB = data;
        }
    }

    // ================= RESOURCE =================
    public static class Resource {
        public String id;
        public double cpuCap, memCap, bwCap;
        public double currLoad;
        public double latencyMs;

        public Resource(String id, double c, double m,
                        double b, double load, double lat) {
            this.id = id;
            cpuCap = c;
            memCap = m;
            bwCap = b;
            currLoad = load;
            latencyMs = lat;
        }
    }

    // ================= MATCHING SCORE =================
    public static double matchingScore(Task t, Resource r,
                                       double a, double b, double g) {

        double sCpu = r.cpuCap / Math.max(1.0, t.cpuReq);
        double sMem = r.memCap / Math.max(1.0, t.memReq);
        double sBw  = r.bwCap  / Math.max(1.0, t.bwReq);

        return a * sCpu + b * sMem + g * sBw;
    }

    // ================= LATENCY =================
    public static double latencyMs(Task t, Resource r) {

        double net = (t.dataSizeMB / r.bwCap) * 1000.0;
        double proc = (t.instrMI / r.cpuCap) * 1000.0;

        return r.latencyMs + net + proc;
    }

    public static double queueDelayMs(Resource r) {
        return r.currLoad * 10.0;
    }

    public static double responseTimeMs(Task t, Resource r) {
        return latencyMs(t, r) + queueDelayMs(r);
    }

    // ================= COST =================
    public static double totalCost(Task t, Resource r) {

        // ✔ Higher cost for cloud
        double cpuUnit = (r.cpuCap >= 6000) ? 0.002 : 0.00025;

        double cpuCost = t.cpuReq * cpuUnit;

        double netCost = (r.latencyMs / 100.0)
                * t.dataSizeMB * 0.0003;

        double transferCost = t.dataSizeMB * 0.0004;

        return cpuCost + netCost + transferCost;
    }

    // ================= LOAD IMBALANCE =================
    public static double computeTau(List<Resource> res) {

        double max = -1e9, min = 1e9, sum = 0;

        for (Resource r : res) {
            max = Math.max(max, r.currLoad);
            min = Math.min(min, r.currLoad);
            sum += r.currLoad;
        }

        double avg = sum / res.size();
        if (avg < 1e-6) avg = 1e-6;

        return (max - min) / avg;
    }
}