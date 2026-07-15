package org.fog.utils;

import java.io.*;
import java.util.*;

public class Dynamic_EMGA {

    private static double W_SCORE;
    private static double W_LATENCY;
    private static double W_COST;
    private static double W_PRIORITY;

    public static class Result {
        public Map<String, List<O2OModel.Task>> allocation = new HashMap<>();
    }

    // ===============================
    // LOAD ML WEIGHTS
    // ===============================
    public static void loadWeights(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));

            W_SCORE = Double.parseDouble(br.readLine());
            W_LATENCY = Double.parseDouble(br.readLine());
            W_COST = Double.parseDouble(br.readLine());
            W_PRIORITY = Double.parseDouble(br.readLine());

            br.close();

            // Normalize weights
            double sum = W_SCORE + W_LATENCY + W_COST + W_PRIORITY;

            W_SCORE /= sum;
            W_LATENCY /= sum;
            W_COST /= sum;
            W_PRIORITY /= sum;

            System.out.println("Dynamic Weights Loaded:");
            System.out.println(W_SCORE + ", " + W_LATENCY + ", " + W_COST + ", " + W_PRIORITY);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Result assignTasks(
            List<O2OModel.Task> tasks,
            List<O2OModel.Resource> resources) {

        Result out = new Result();

        for (O2OModel.Resource r : resources)
            out.allocation.put(r.id, new ArrayList<>());

        tasks.sort((a, b) -> Double.compare(b.priority, a.priority));

        for (O2OModel.Task t : tasks) {

            O2OModel.Resource best = null;
            double bestU = -1e9;

            for (O2OModel.Resource r : resources) {

                if (r.cpuCap < t.cpuReq || r.memCap < t.memReq)
                    continue;

                double score = O2OModel.matchingScore(t, r, 0.5, 0.3, 0.2);
                double latency = O2OModel.latencyMs(t, r);
                double cost = O2OModel.totalCost(t, r);
                double priority = t.priority / 5.0;

                // SAME LOGIC (VERY IMPORTANT)
                double ns = Math.tanh(score / 5.0);
                double nl = latency / 50.0;
                double nc = cost;
                double np = priority;

                double loadPenalty = r.currLoad;

                double U = (W_SCORE * ns)
                        - (W_LATENCY * nl)
                        - (W_COST * nc)
                        + (W_PRIORITY * np)
                        - (0.3 * loadPenalty);

                if (r.id.contains("Edge")) {
                    U += 0.05;
                }

                if (U > bestU) {
                    bestU = U;
                    best = r;
                }
            }

            if (best != null) {
                out.allocation.get(best.id).add(t);

                best.currLoad += 0.15;
            }
        }

        printResult(out);
        return out;
    }

    private static void printResult(Result out) {

        System.out.println("\nAllocation:");

        for (String r : out.allocation.keySet()) {
            System.out.println(r + " -> " + out.allocation.get(r).size() + " tasks");
        }
    }
}