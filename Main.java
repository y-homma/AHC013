import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

class Main {
      public static void main(String[] args) {
            var scanner = new Scanner(System.in);
            var writer = new PrintWriter(System.out);
            var isLocal = false;
            try {
                  var outFileName = "./temp.txt";
                  if (args.length > 0) {
                              scanner = new Scanner(new File(args[0]));
                              outFileName = args[0].replaceAll("in", "out");
                              isLocal = true;
                  }
                  var filewriter = new FileWriter(new File(outFileName));
                  var N = readInt(scanner);
                  var K = readInt(scanner);
                  var C = new int[N][N];
                  for (var i = 0; i < N; i++) {
                        var cString = scanner.next();
                        for (var j = 0; j < N; j++) {
                        C[i][j] = Integer.valueOf(cString.substring(j, j+1));
                        }
                  }
                  var ans = solve(N, K, C);
                  for (var ansStr : ans) {
                        if (!isLocal) {
                              writer.println(ansStr);
                        } else {
                              filewriter.write(ansStr + "\n");
                        }     
                  }
                  filewriter.close();
            } catch (FileNotFoundException  e) {
                  System.err.println("file not found");
            } catch (IOException e) {
                  System.err.println("file unable to write");
            }

            scanner.close();
            writer.flush();
      }

      static ArrayList<String> solve(final int N, final int K, final int[][] C) {
            var ans = new ArrayList<String>();
            // 移動
            var moveResult = move(N, K, C);
            ans.addAll(moveResult.getMoves());
            // 結合
            var connectString = connect(N, 100 * K - moveResult.getMoves().size(), moveResult.getRoom());
            ans.addAll(connectString);
            return ans;
      }

      static moveResult move(final int N, final int K, final int[][] C) {
            var start = System.currentTimeMillis();
            var room = new int[N][N];
            for (var i = 0; i < N; i++) {
                  for (var j = 0; j < N; j++) {
                        room[i][j] = C[i][j];
                  }
            }
            var move = new ArrayList<int[]>();
            var rand = new Xoroshiro128pp();
            var diff = new int[] { 0, -1, 0, 1, 0};
            var rot = new int[] { -1, -1, 1, 1, -1};
            var lim = (K - 1) * 75;
            var congestion = N * N / (100.0 * K);
            var limTime = 2000.0; //ms
            while (true) {
                  var curTime = System.currentTimeMillis() - start;
                  if (curTime > 2000) {
                  break;
                  }
                  var temp = 1.0 * (1 - curTime/ limTime);
                  for (var iroop = 0; iroop < 100; iroop++) {     
                        var operation = rand.nextInt(10);
                        if (operation <= 7 && move.size() < (K - 1) * 100) {
                              // 移動の追加
                              var ci = rand.nextInt(N); var cj = rand.nextInt(N);
                              var n = room[ci][cj];
                              if (n == 0) {
                                    var rIdx = rand.nextInt(4);
                                    var di = rot[rIdx]; var dj = rot[rIdx + 1];
                                    if (!(isValid(N, ci + di, cj) && isValid(N, ci, cj + dj) && isValid(N, ci + di, cj + dj))) {
                                          continue;
                                    } else if (room[ci + di][cj] == 0 && room[ci][cj + dj] == 0 && room[ci + di][cj + dj] == 0) {
                                          continue;
                                    }
                                    var leftRotate = rand.nextInt(2) == 0;
                                    var beforeConnect = calcConnect(room, ci, cj, N, K) + calcConnect(room, ci + di, cj, N, K) + calcConnect(room, ci, cj + dj, N, K) + calcConnect(room, ci + di, cj + dj, N, K);
                                    var rotateMoves = rotate(room, ci, cj, di, dj, leftRotate);
                                    var afterConnect = calcConnect(room, ci, cj, N, K) + calcConnect(room, ci + di, cj, N, K) + calcConnect(room, ci, cj + dj, N, K) + calcConnect(room, ci + di, cj + dj, N, K);
                                    var point = afterConnect - beforeConnect - Math.pow(Math.exp((move.size() + 3) / (lim * 0.9)), 1 + congestion);
                                    if (point > 0 || Math.exp(point / temp) > rand.nextInt() / (double) Integer.MAX_VALUE) {
                                          move.addAll(rotateMoves);
                                    } else {
                                          rotate(room, ci, cj, di, dj, !leftRotate); 
                                    }
                              } else {
                                    var didx = rand.nextInt(4);
                                    var di = diff[didx]; var dj = diff[didx + 1];
                                    var ni = ci + di; var nj = cj + dj;
                                    if (!isValid(N, ni, nj) || room[ni][nj] != 0) {
                                    // 移動できない場合は何もしない
                                          continue;
                                    }
                                    var beforeConnect = calcConnect(room, ci, cj, N, K);
                                    room[ci][cj] = 0;
                                    room[ni][nj] = n;
                                    var point = calcConnect(room, ni, nj, N, K) - beforeConnect - Math.pow(Math.exp((move.size() + 1) / (lim * 0.9)), 2.5);
                                    if (point > 0 || Math.exp(point / temp) > rand.nextInt() / (double) Integer.MAX_VALUE) {
                                          move.add(new int[] {n, ci, cj, ni, nj});
                                    } else {
                                          room[ci][cj] = n;
                                          room[ni][nj] = 0;   
                                    }
                              }
                        } else {
                              if (move.isEmpty() || curTime > limTime - 200) {
                                    continue;
                              }
                              // 移動の削除
                              var opId = rand.nextInt(move.size());
                              var beforeDelete = calcPoint(N, room, K);
                              var restore = deleteMove(opId, move, C);
                              var afterDelete = calcPoint(N, restore.getRoom(), K);
                              var point = afterDelete - beforeDelete + (lim - restore.getMoves().size()) * (1 - (curTime / (limTime - 200)));
                              // System.err.println(String.format("delete point: %s, before* %s", afterDelete, beforeDelete));
                              if (point > 0 || Math.exp(point / temp) > rand.nextInt() / (double) Integer.MAX_VALUE) {
                                    move = restore.getMoves();
                                    room = restore.getRoom();
                              } 
                        }
                  }
            }
            move = compress(N, move);
            var ans = new ArrayList<String>();
            ans.add(String.valueOf(move.size()));
            for (var m : move) {
                  ans.add(String.format("%d %d %d %d", m[1], m[2], m[3], m[4]));
            }
            return new moveResult(ans, room);
      }

      static ArrayList<String> connect(final int N, final int K, final int[][] C) {
            var isEnable = new int[N][N];
            var servers = new HashMap<Integer, ArrayList<int[]>>();
            var maxN = 0;
            for (var i = 0; i < N; i++) {
                  for (var j = 0; j < N; j++) {
                        var n = C[i][j];
                        isEnable[i][j] = n;
                        if (n != 0) {
                              if (!servers.containsKey(n)) {
                                    servers.put(n, new ArrayList<>());
                                    maxN = Math.max(n, maxN);
                              } 
                              servers.get(n).add(new int[] { i, j });
                        }
                  }
            }
            var edges = new ArrayList<int[]>();
            for (var entry : servers.entrySet()) {
                  var n = entry.getKey();
                  var nodes = entry.getValue();
                  for (var i = 0; i < nodes.size() - 1; i++) {
                        var f = nodes.get(i);
                        for (var j = i + 1; j < nodes.size(); j++) {
                              var t = nodes.get(j);
                              if (isConnectable(isEnable, f[0], f[1], t[0], t[1])) {
                                    edges.add(new int[] {i, j, n, calcDist(f[0], f[1], t[0], t[1])});
                              }
                        }
                  }
            }
            var connects = new ArrayList<String>();
            while (connects.size() < K && edges.size() > 0) {
                  var ufts = new UFT[maxN + 1];
                  var maxCount = 1;
                  var startN = 0;
                  var startNode = -1;
                  // uftで連結成分の個数をカウントする
                  var counts = new HashSet<Integer>();
                  for (var i = 1; i < maxN + 1; i++) {
                        ufts[i] = new UFT(servers.get(i).size());
                  }
                  for (var edge : edges) {
                        ufts[edge[2]].unite(edge[0], edge[1]);
                  }

                  for (var i = 1; i < maxN + 1; i++) {
                        var uft = ufts[i];
                        for (var j = 0; j < servers.get(i).size(); j++) {
                              var parent = uft.find(j);
                              if (!counts.contains(i * 1000 + parent)) {
                                    var thisCount = uft.count(parent);
                                    counts.add(i * 1000 + parent);
                                    if (thisCount > maxCount) {
                                          maxCount = thisCount;
                                          startN = i;
                                          startNode = parent;
                                    }
                              }
                        }
                  }
                  if (maxCount == 1) {
                        break;
                  }

                  // 最大の連結成分を持つ者に対し、プリム法で最小全域木を作成する
                  var S = servers.get(startN);
                  // 辺の抽出
                  var thisEdges = new HashMap<Integer, HashSet<Integer>>();
                  for (var edge : edges) {
                        if (edge[2] == startN) {
                              if (!thisEdges.containsKey(edge[0])) {
                                    thisEdges.put(edge[0], new HashSet<>());
                              }
                              if (!thisEdges.containsKey(edge[1])) {
                                    thisEdges.put(edge[1], new HashSet<>());
                              }
                              thisEdges.get(edge[0]).add(edge[1]);
                              thisEdges.get(edge[1]).add(edge[0]);
                        }
                  }
                  // プリム法
                  var pq = new PriorityQueue<int[]>(Comparator.comparing(vct -> vct[0], Comparator.naturalOrder()));
                  var startCood = S.get(startNode);
                  for (var next : thisEdges.get(startNode)) {
                        var nn = S.get(next);
                        pq.add(new int[] {calcDist(startCood[0], startCood[1], nn[0], nn[1]), next, startNode});
                  }
                  var used = new boolean[S.size()];
                  Arrays.fill(used, false);
                  used[startNode] = true;
                  while (!pq.isEmpty()) {   
                        var cn = pq.poll();
                        var tidx = cn[1];
                        var fidx = cn[2];
                        var t = S.get(tidx);
                        var f = S.get(fidx);
                        if (!used[tidx] && isConnectable(isEnable, f[0], f[1], t[0], t[1])) {
                              used[tidx] = true;
                              connects.add(String.format("%d %d %d %d", t[0], t[1], f[0], f[1]));
                              if (f[0] == t[0]) {
                                    for (var k = Math.min(f[1], t[1]); k <= Math.max(f[1], t[1]); k++) {
                                          isEnable[f[0]][k] = -1 * startN;
                                    }
                              } else {
                                    for (var k = Math.min(f[0], t[0]); k <= Math.max(f[0], t[0]); k++) {
                                          isEnable[k][f[1]] = -1 * startN;
                                    }                                               
                              }  
                              for (var next : thisEdges.get(tidx)) {
                                    if (!used[next]) {
                                          var nn = S.get(next);
                                          pq.add(new int[] {calcDist(t[0], t[1], nn[0], nn[1]), next, tidx});
                                          // System.err.println(String.format("Add edge (%d, %d) to (%d, %d)", t[0], t[1], nn[0], nn[1]));
                                    }
                              }
                        }
                  }
                  // 辺の初期化
                  edges.clear();
                  for (var entry : servers.entrySet()) {
                        var n = entry.getKey();
                        var nodes = entry.getValue();
                        for (var i = 0; i < nodes.size() - 1; i++) {
                              var f = nodes.get(i);
                              if (isEnable[f[0]][f[1]] <= 0) {
                                    continue;
                              }
                              for (var j = i + 1; j < nodes.size(); j++) {
                                    var t = nodes.get(j);
                                    if (isEnable[t[0]][t[1]] <= 0) {
                                          continue;
                                    }
                                    if (isConnectable(isEnable, f[0], f[1], t[0], t[1])) {
                                          edges.add(new int[] {i, j, n, calcDist(f[0], f[1], t[0], t[1])});
                                    }
                              }
                        }
                  }
            }
            var ans = new ArrayList<String>();
            ans.add(String.valueOf(Math.min(connects.size(), K)));
            ans.addAll(connects.subList(0, Math.min(connects.size(), K)));
            return ans;
      }

      static boolean isValid(final int N, final int i, final int j) {
            if (i < 0 || N <= i || j < 0 || N <= j) {
                  return false;
            }
            return true;
      }

      static List<int[]> rotate(int[][] C, final int i, final int j, final int di, final int dj, final boolean isLeftRotate) {
            var moves = new ArrayList<int[]>();
            if (di * dj == 1) { // 左
                  if (isLeftRotate) {
                        moves.add(new int[] { C[i][j + dj], i, j + dj, i, j});
                        moves.add(new int[] { C[i + di][j + dj], i + di, j + dj, i, j + dj});
                        moves.add(new int[] { C[i + di][j], i + di, j, i + di, j + dj});
                        var temp = C[i][j];
                        C[i][j] = C[i][j + dj];
                        C[i][j+dj] = C[i+di][j+dj];
                        C[i+di][j+dj] = C[i+di][j];
                        C[i+di][j] = temp;
                  } else {
                        moves.add(new int[] { C[i + di][j], i + di, j, i, j});
                        moves.add(new int[] { C[i + di][j + dj], i + di, j + dj, i + di, j});
                        moves.add(new int[] { C[i][j + dj], i, j + dj, i + di, j + dj});
                        var temp = C[i][j];
                        C[i][j] = C[i+di][j];
                        C[i+di][j] = C[i+di][j+dj];
                        C[i+di][j+dj] = C[i][j+dj];
                        C[i][j+dj] = temp;
                  }
            } else {
                  if (isLeftRotate) {
                        moves.add(new int[] { C[i + di][j], i + di, j, i, j});
                        moves.add(new int[] { C[i + di][j + dj], i + di, j + dj, i + di, j});
                        moves.add(new int[] { C[i][j + dj], i, j + dj, i + di, j + dj});
                        var temp = C[i][j];
                        C[i][j] = C[i+di][j];
                        C[i+di][j] = C[i+di][j+dj];
                        C[i+di][j+dj] = C[i][j+dj];
                        C[i][j+dj] = temp;
                  } else {
                        moves.add(new int[] { C[i][j + dj], i, j + dj, i, j});
                        moves.add(new int[] { C[i + di][j + dj], i + di, j + dj, i, j + dj});
                        moves.add(new int[] { C[i + di][j], i + di, j, i + di, j + dj});
                        var temp = C[i][j];
                        C[i][j] = C[i][j + dj];
                        C[i][j+dj] = C[i+di][j+dj];
                        C[i+di][j+dj] = C[i+di][j];
                        C[i+di][j] = temp;
                  }
            }
            return moves.stream().filter(vec -> vec[0] > 0).collect(Collectors.toList());
      }

      static int calcConnect(final int[][] C, final int i, final int j, final int N, final int K) {
            var n = C[i][j];
            if (n == 0) {
                  return 0;
            }
            var connect = 0;
            var base = (K - n + 1) * (K - n + 1);
            var point = 0;
            var top = 0;
            for (var si = i - 1; si >= 0; si--) {
                  if (C[si][j] == n) {
                        connect += 1;
                        break;
                  } else if (C[si][j] != 0) {
                        top = C[si][j];
                        point  -= (K - top + 1) * (K - top + 1);
                        break;
                  }
            }
            for (var si = i + 1; si < N; si++) {
                  if (C[si][j] == n) {
                        connect += 1;
                        break;
                  } else if (C[si][j] != 0) {
                        if (top == C[si][j]) {
                              point -= (K - top + 1) * (K - C[si][j] + 1) * 2;
                        } else {
                              point -= (K - C[si][j] + 1) * (K - C[si][j] + 1) ;
                        }
                        break;
                  }
            }
            var left = 0;
            for (var sj = j - 1; sj >= 0; sj--) {
                  if (C[i][sj] == n) {
                        connect += 1;
                        break;
                  } else if (C[i][sj] != 0) {
                        left = C[i][sj];
                        point -= (K - left + 1) * (K - left + 1);
                        break;
                  }
            }
            for (var sj = j + 1; sj < N; sj++) {
                  if (C[i][sj] == n) {
                        connect += 1;
                        break;
                  } else if (C[i][sj] != 0) {
                        if (left == C[i][sj]) {
                              point -= (K - left + 1) * (K - C[i][sj] + 1) * 2;
                        } else {
                              point -= (K - C[i][sj] + 1) * (K - C[i][sj] + 1);
                        }
                        break;
                  }
            }
            point += base * connect * (connect + 1);
            return point;
      }

      static int findConnect(final int N, final int[][] C, final int i, final int j, boolean[][] isVisited) {
            var k = C[i][j];
            var dq = new ArrayDeque<int[]>();
            dq.add(new int[] { i, j });
            var connect = 0;
            while (!dq.isEmpty()) {
                  var cur = dq.poll();
                  var ci = cur[0]; var cj = cur[1];
                  if (!isVisited[ci][cj]) {
                        connect += 1;
                        isVisited[ci][cj] = true;
                        // up
                        for (int ni = ci - 1; ni >= 0; ni--) {
                              if (C[ni][cj] == k) {
                                    if (!isVisited[ni][cj]) {
                                          dq.add(new int[] {ni, cj});
                                    }
                                    break;
                              } else if (C[ni][cj] != 0){
                                    break;
                              }
                        }
                         // down
                        for (int ni = ci + 1; ni < N; ni++) {
                              if (C[ni][cj] == k) {
                                    if (!isVisited[ni][cj]) {
                                          dq.add(new int[] {ni, cj});
                                    }
                                    break;
                              } else if (C[ni][cj] != 0){
                                    break;
                              }
                        }
                        // left
                        for (int nj = cj - 1; nj >= 0; nj--) {
                              if (C[ci][nj] == k) {
                                    if (!isVisited[ci][nj]) {
                                          dq.add(new int[] {ci, nj});
                                    }
                                    break;
                              } else if (C[ci][nj] != 0){
                                    break;
                              }
                        }
                        // right
                        for (int nj = cj + 1; nj < N; nj++) {
                              if (C[ci][nj] == k) {
                                    if (!isVisited[ci][nj]) {
                                          dq.add(new int[] {ci, nj});
                                    }
                                    break;
                              } else if (C[ci][nj] != 0){
                                    break;
                              }
                        }
                  }
            }
            return connect;
      }

      static int calcPoint(final int N, final int[][]C, final int K) {
            var isVisited = new boolean[N][N];
            for (var vct : isVisited) {
                  Arrays.fill(vct, false);
            }
            var point = 0;
            for (var i = 0; i < N; i++) {
                  for (var j = 0; j < N; j++) {
                        if (C[i][j] > 0 && !isVisited[i][j]) {
                              var connect = findConnect(N, C, i, j, isVisited);
                              point += (K - C[i][j] + 1) * (K - C[i][j] + 1) * (connect - 1) * connect / 2;
                        }
                  }
            }
            return point;
      }

      static Restore deleteMove(final int index, final ArrayList<int[]> move, final int[][] C) {
                  var newMove = new ArrayList<int[]>();
                  var r = new int[C.length][C.length];
                  for (var i = 0; i < C.length; i++) {
                        for (var j = 0; j < C.length; j++) {
                              r[i][j] = C[i][j];
                        }
                  }
                  for (var i = 0; i < move.size(); i++) {
                        var m = move.get(i);
                        if (i != index && r[m[1]][m[2]] == m[0] && r[m[3]][m[4]] == 0) {
                              newMove.add(m);
                              r[m[1]][m[2]] = 0;
                              r[m[3]][m[4]] = m[0];
                        }
                  }
                  return new Restore(newMove, r);
      } 

      static ArrayList<int[]> compress(final int N, final ArrayList<int[]> moves) {
            var lastMove = new int[N][N];
            for (var i = 0; i < N; i++) {
                  Arrays.fill(lastMove[i], -1);
            }
            var delete = new HashSet<Integer>();
            for (var i = 0; i < moves.size(); i++) {
                  var move = moves.get(i);
                  var fi = move[1]; var fj = move[2];
                  var ti = move[3]; var tj = move[4];
                  if (lastMove[fi][fj] == lastMove[ti][tj] && lastMove[fi][fj] != -1) {
                        delete.add(lastMove[fi][fj]);
                        delete.add(i);
                        lastMove[fi][fj] = -1;
                        lastMove[ti][tj] = -1;
                  } else {
                        lastMove[fi][fj] = lastMove[ti][tj] = i;
                  }
            }

            var newMoves = new ArrayList<int[]>();
            for (var i = 0; i < moves.size(); i++) {
                  if (!delete.contains(i)) {
                        newMoves.add(moves.get(i));
                  }
            }
            // System.out.println(String.format("compressed %d moves", delete.size()));
            return delete.isEmpty() ? newMoves : compress(N, newMoves);
      }

      static int calcDist(final int fi, final int fj, final int ti, final int tj) {
            return fi == ti ? Math.abs(fj - tj) : Math.abs(fi - ti);
      }

      static boolean isConnectable(final int[][] C, final int fi, final int fj, final int ti, final int tj) {
            if (fi == ti) {
                  for (var k = Math.min(fj, tj) + 1; k < Math.max(fj, tj); k++) {
                        if (C[fi][k] != 0) {
                              return false;
                        }
                  }
                  return true;
            } else if (fj == tj) {
                  for (var k = Math.min(fi, ti) + 1; k < Math.max(fi, ti); k++) {
                        if (C[k][fj] != 0) {
                              return false;
                        }
                  }
                  return true;
            } else {
                  return false;
            }
      }

      /**
       * 入力をInt型で受け取ります
       * @param scanner Scanner
       * @return 入力値(int)
       */
      static Integer readInt(final Scanner scanner) {
            return Integer.parseInt(scanner.next());
      }

      static class moveResult {
            private ArrayList<String> moves;
            private int[][] room;
            public moveResult(final ArrayList<String> moves, final int[][] room) {
                  this.moves = moves;
                  this.room = room;
            }
            public ArrayList<String> getMoves() {
                  return this.moves;
            }
            public int[][] getRoom() {
                  return this.room;
            }
      }

      static class Restore {
            private ArrayList<int[]> moves;
            private int[][] room;
            public Restore(final ArrayList<int[]> moves, final int[][] room) {
                  this.moves = moves;
                  this.room = room;
            }
            public ArrayList<int[]> getMoves() {
                  return this.moves;
            }
            public int[][] getRoom() {
                  return this.room;
            }
      }

      static class UFT {
            private int[] rank;
            private int[] tree;
            public UFT(final Integer N) {
                  this.tree = new int[N];
                  this.rank = new int[N];
                  for (var i = 0; i < N; i++) {
                        this.tree[i] = i;
                        this.rank[i] = 0;
                  }
            }

            public int find(final Integer index) {
                  if (this.tree[index] == index) {
                        return index;
                  } else {
                        this.tree[index] = find(this.tree[index]);
                        return this.tree[index];
                  }
            }

            public void unite(final Integer a, final Integer b) {
                  var pa = find(a);
                  var pb = find(b);
                  if (pa == pb) {
                        return;
                  }
                  if (this.rank[pa] < this.rank[pb]) {
                        this.tree[pa] = pb;
                  } else {
                        this.tree[pb] = pa;
                        if (this.rank[pa] == this.rank[pb]) {
                              this.rank[pa] += 1;
                        }
                  }
            }

            public boolean isConnect(final Integer a, final Integer b) {
                  return find(a) == find(b);
            }

            public int count(final Integer a) {
                  var par = find(a);
                  var c = 0;
                  for (var i = 0; i < this.tree.length; i++) {
                        if (par == find(i)) {
                              c += 1;
                        }
                  }
                  return c;
            }
      }

      /**
       * 乱数生成器(xoroshiro128++)
       */
      static class Xoroshiro128pp {
            private long[] s = {ThreadLocalRandom.current().nextLong(), 0};
            private long rotl(final long x, final int k) {
                  return (x << k) | (x >>> (64 - k));
            }
            private long xoroshiro128() {
                  var s0 = s[0];
                  var s1 = s[1];
                  var result = rotl(s0 + s1, 17) + s0;
                  s1 ^= s0;
                  s[0] = rotl(s0, 49) ^ s1 ^ (s1 << 21);
                  s[1] = rotl(s1, 28);
                  return result;
            }
            private long nextP2(final long n) {
                  var r = n - 1;
                  for (var i = 0; i < 6; i++) {
                        r |= r >>> (1 << i);
                  }
                  return r;
            }
            // 指定した値を最大値として乱数を生成します
            public int nextInt(final int mod) {
                  var res = 0L;
                  var p2mod = nextP2((long) mod);
                  do {
                        res = xoroshiro128() & p2mod;
                  } while (res >= mod);
                  return (int) res;
            }
            // int型の範囲で乱数を生成します
            public int nextInt() {
                  return nextInt(Integer.MAX_VALUE);
            }
      }
}
