package org.fog.test.perfeval;

import org.fog.utils.*;
import java.util.*;

public class EMGA_Comparison {

    public static void main(String[] args) {
        runComparison();
    }

    public static void runComparison() {

        // ===============================
        // CREATE TASKS
        // ===============================
        List<O2OModel.Task> tasks = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            tasks.add(new O2OModel.Task(
                    1 + Math.random()*4,
                    2000 + Math.random()*4000,
                    100 + Math.random()*400,
                    64 + Math.random()*256,
                    10 + Math.random()*50,
                    100 + Math.random()*200,
                    5 + Math.random()*20
            ));
        }

        // ===============================
        // CREATE RESOURCES
        // ===============================
        List<O2OModel.Resource> resources = new ArrayList<>();

        resources.add(new O2OModel.Resource("Edge-1", 2000, 1024, 100, 0.2, 10));
        resources.add(new O2OModel.Resource("Edge-2", 2500, 2048, 120, 0.3, 12));
        resources.add(new O2OModel.Resource("Fog-1", 4000, 4096, 200, 0.4, 20));
        resources.add(new O2OModel.Resource("Cloud", 8000, 8192, 500, 0.5, 50));

        // ===============================
        // STATIC EMGA
        // ===============================
        System.out.println("\n===== STATIC EMGA =====");
        EMGA.Result staticRes = EMGA.assignTasks(tasks, resources);

        Metrics staticMetrics = calculateMetrics(staticRes, resources);

        // ===============================
        // DYNAMIC EMGA
        // ===============================
        System.out.println("\n===== DYNAMIC EMGA =====");

        Dynamic_EMGA.loadWeights(
                "D:\\Paper 2 - (Implementation)\\weights.txt"
        );

        Dynamic_EMGA.Result dynamicRes =
                Dynamic_EMGA.assignTasks(tasks, resources);

        Metrics dynamicMetrics = calculateMetrics(dynamicRes, resources);

        // ===============================
        // FINAL COMPARISON
        // ===============================
        printComparison(staticMetrics, dynamicMetrics);
    }

    // ===============================
    // METRICS CLASS
    // ===============================
    static class Metrics {
        double responseTime;
        double cost;
        double loadImbalance;
    }

    // ===============================
    // CALCULATE METRICS
    // ===============================
    public static Metrics calculateMetrics(Object result,
                                           List<O2OModel.Resource> resources) {

        Metrics m = new Metrics();

        Map<String, List<O2OModel.Task>> allocation;

        if (result instanceof EMGA.Result) {
            allocation = ((EMGA.Result) result).allocation;
        } else {
            allocation = ((Dynamic_EMGA.Result) result).allocation;
        }

        double totalRT = 0;
        double totalCost = 0;
        int count = 0;

        for (O2OModel.Resource r : resources) {

            List<O2OModel.Task> tasks = allocation.get(r.id);

            if (tasks == null) continue;

            for (O2OModel.Task t : tasks) {

                totalRT += O2OModel.responseTimeMs(t, r);
                totalCost += O2OModel.totalCost(t, r);
                count++;
            }
        }

        m.responseTime = totalRT / count;
        m.cost = totalCost / count;
        m.loadImbalance = O2OModel.computeTau(resources);

        return m;
    }

    // ===============================
    // PRINT COMPARISON
    // ===============================
    public static void printComparison(Metrics s, Metrics d) {

        System.out.println("\n========== FINAL COMPARISON ==========");

        System.out.println("\n--- RESPONSE TIME ---");
        System.out.println("Static EMGA:  " + s.responseTime);
        System.out.println("Dynamic EMGA: " + d.responseTime);

        System.out.println("\n--- COST ---");
        System.out.println("Static EMGA:  " + s.cost);
        System.out.println("Dynamic EMGA: " + d.cost);

        System.out.println("\n--- LOAD IMBALANCE ---");
        System.out.println("Static EMGA:  " + s.loadImbalance);
        System.out.println("Dynamic EMGA: " + d.loadImbalance);

        System.out.println("\n🏆 BETTER MODEL:");

        if (d.responseTime < s.responseTime)
            System.out.println("✔ Dynamic EMGA improves Response Time");

        if (d.cost < s.cost)
            System.out.println("✔ Dynamic EMGA reduces Cost");

        if (d.loadImbalance < s.loadImbalance)
            System.out.println("✔ Dynamic EMGA improves Load Balance");
    }
}