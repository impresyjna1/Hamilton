import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jakub on 11.10.2016.
 */
public class HalfTSPLocalSearch {
    public static HalfTSPResult_LocalSearch localSearch(Graph graph, int initialVertex, BiFunction<Graph, Integer, HalfTSPResult> algorithm) {
        boolean found;
        HalfTSPResult initialResult = algorithm.apply(graph, initialVertex);
        ArrayList<Integer> cycle = new ArrayList<>(initialResult.getPath());
        int distance = initialResult.getDistance();
        List<Integer> freeVertices = IntStream.range(0, graph.getVerticesCount()).filter(v -> !cycle.contains(v)).boxed().collect(Collectors.toList());
        int verticesInCycleCount = graph.getVerticesCount() / 2;
        int[][] adjacencyMatrix = graph.getAdjacencyMatrix();

        do {
            found = false;
            int minChange = 0;
            int vertex1IndexToChange = -1;
            int vertex2IndexToChange = -1;
            int vertexToReplace = -1;
            boolean changeInsideCycle = false;

            for (int index1 = 0; index1 < verticesInCycleCount; ++index1) {
                int vertex1 = cycle.get(index1);
                int vertex1Next = cycle.get((index1 + 1) % verticesInCycleCount);
                int vertex1Prev = cycle.get((index1 + verticesInCycleCount - 1) % verticesInCycleCount);

                //wymiana łuków 2-opt
                for (int index2 = index1 + 2; index2 < verticesInCycleCount && index2 != (index1 + verticesInCycleCount - 1) % verticesInCycleCount; ++index2) {
                    int vertex2 = cycle.get(index2);
                    int vertex2Next = cycle.get((index2 + 1) % verticesInCycleCount);
                    int change = adjacencyMatrix[vertex1][vertex2] + adjacencyMatrix[vertex1Next][vertex2Next] -
                            adjacencyMatrix[vertex1][vertex1Next] - adjacencyMatrix[vertex2][vertex2Next];
                    if (change < minChange) {
                        minChange = change;
                        vertex1IndexToChange = index1;
                        vertex2IndexToChange = index2;
                        changeInsideCycle = true;
                    }
                }

                //wymiana wierzchołków 2-opt
                for (int vertex2 : freeVertices) {
                    int change = adjacencyMatrix[vertex2][vertex1Prev] + adjacencyMatrix[vertex2][vertex1Next] -
                            adjacencyMatrix[vertex1][vertex1Prev] - adjacencyMatrix[vertex1][vertex1Next];
                    if (change < minChange) {
                        minChange = change;
                        vertex1IndexToChange = index1;
                        vertexToReplace = vertex2;
                        changeInsideCycle = false;
                    }
                }
            }

            if (minChange < 0) {
                found = true;
                if (changeInsideCycle) {
                    ArrayList<Integer> subPath = new ArrayList<>(cycle.subList(vertex1IndexToChange + 1, vertex2IndexToChange + 1));
                    cycle.removeAll(subPath);
                    Collections.reverse(subPath);
                    cycle.addAll(vertex1IndexToChange + 1, subPath);
                } else {
                    freeVertices.add(cycle.get(vertex1IndexToChange));
                    freeVertices.remove(new Integer(vertexToReplace));
                    cycle.remove(vertex1IndexToChange);
                    cycle.add(vertex1IndexToChange, vertexToReplace);
                }
                distance += minChange;
            }
        } while (found);
        return new HalfTSPResult_LocalSearch(distance, cycle, initialResult);
    }

    public static HalfTSPResult_LocalSearch multipleStartLocalSearch(Graph graph, BiFunction<Graph, Integer, HalfTSPResult> algorithm, int repeatsCount) {
        Random generator = new Random();
        int verticesCount = graph.getVerticesCount();
        HalfTSPResult_LocalSearch minResult = null;
        for (int i = 0; i < repeatsCount; ++i) {
            HalfTSPResult_LocalSearch currentResult = localSearch(graph, generator.nextInt(verticesCount), algorithm);
            if (minResult == null || currentResult.getDistance() < minResult.getDistance()) {
                minResult = currentResult;
            }
        }
        return minResult;
    }

    public static HalfTSPResult_LocalSearch iteratedLocalSearch(Graph graph, BiFunction<Graph, Integer, HalfTSPResult> algorithm, long timeRestriction) {
        Random generator = new Random();
        int verticesCount = graph.getVerticesCount();
        long startTime = System.nanoTime();
        long endTime;
        HalfTSPResult_LocalSearch minResult = localSearch(graph, generator.nextInt(verticesCount), algorithm);
        do {
            HalfTSPResult perturbation = perturbation(graph, minResult);
            HalfTSPResult_LocalSearch currentResult = localSearch(graph, -1, (g, initialVertex) -> perturbation);
            if (currentResult.getDistance() < minResult.getDistance()) {
                minResult.setPath(currentResult.getPath());
                minResult.setDistance(currentResult.getDistance());
            }
            endTime = System.nanoTime();
        } while (endTime - startTime < timeRestriction);
        return minResult;
    }

    private static HalfTSPResult perturbation(Graph graph, HalfTSPResult result) {
        Random generator = new Random();
        int distance = result.getDistance();
        ArrayList<Integer> cycle = new ArrayList<>(result.getPath());
        int verticesInCycleCount = cycle.size();
        List<Integer> freeVertices = IntStream.range(0, graph.getVerticesCount()).filter(v -> !cycle.contains(v)).boxed().collect(Collectors.toList());
        int[][] adjacencyMatrix = graph.getAdjacencyMatrix();

        for (int i = 0; i < 2; ++i) {
            int vertex1Index = generator.nextInt(cycle.size());
            int vertex1 = cycle.get(vertex1Index);
            int vertex1Prev = cycle.get((vertex1Index + verticesInCycleCount - 1) % verticesInCycleCount);
            int vertex1Next = cycle.get((vertex1Index + 1) % verticesInCycleCount);
            int vertex2 = freeVertices.get(generator.nextInt(freeVertices.size()));

            int change = adjacencyMatrix[vertex1Prev][vertex2] + adjacencyMatrix[vertex2][vertex1Next] -
                    adjacencyMatrix[vertex1Prev][vertex1] - adjacencyMatrix[vertex1][vertex1Next];

            freeVertices.add(vertex1);
            freeVertices.remove(new Integer(vertex2));
            cycle.remove(new Integer(vertex1));
            cycle.add(vertex1Index, vertex2);
            distance += change;
        }

        return new HalfTSPResult(distance, cycle);
    }
}
