package traffic;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

class Menu{
  private int numberOfRoads;
  private int interval;
  Scanner sc;

  public Menu(){
    sc = new Scanner(System.in);
    askInitialData();
  }

  public int getNumberOfRoads() {
    return numberOfRoads;
  }

  public int getInterval(){
    return interval;
  }

  public void showMenu(){
    clearConsole();
    System.out.println("Menu:");
    System.out.println("1. Add");
    System.out.println("2. Delete");
    System.out.println("3. System");
    System.out.println("0. Quit");
  }

  private int readInteger(int validMin, int validMax, String message){
    boolean ok = false;
    do {
      try {
        int input = sc.nextInt();
        if (input < validMin || input > validMax) throw new Exception();
        ok = true;
        return input;
      } catch (Exception e) {
        System.out.print("Error! Incorrect input. Try again: ");
        sc.nextLine();
      }
    }while (!ok);
    return -1;
  }

  public void clearConsole(){
    try {
      var clearCommand = System.getProperty("os.name").contains("Windows")
              ? new ProcessBuilder("cmd", "/c", "cls")
              : new ProcessBuilder("clear");
      clearCommand.inheritIO().start().waitFor();
    }
    catch (IOException | InterruptedException e) {}

  }

  public void askInitialData(){
    clearConsole();
    System.out.println("Welcome to the traffic management system!");
    System.out.print("Input the number of roads: ");
    this.numberOfRoads = readInteger(1, Integer.MAX_VALUE, "Error! Incorrect input. Try again: ");
    System.out.print("Input the interval: ");
    this.interval = readInteger(1, Integer.MAX_VALUE, "Error! Incorrect input. Try again: ");
  }

  public int doMenu(){
    this.showMenu();
    Integer[] array = {0, 1, 2, 3};
    ArrayList<Integer> answers = new ArrayList<>(Arrays.asList(array));
    int choice = -1;
    try {
      choice = sc.nextInt();
      if(!answers.contains(choice)) throw new Exception();
      return choice;
    }catch(Exception e) {
      sc.nextLine();
      System.out.println("Incorrect option");
      sc.nextLine();
    }
    return -1;
  }

}

enum MenuState{
  NOT_STARTED,
  MENU,
  SYSTEM,
  END
}
class Road{
  enum RoadState{
    OPEN,
    CLOSE
  }
  private String name;
  public int time;
  public RoadState state = RoadState.CLOSE;
  public boolean isActive = false;

  public Road(String name){
    this.name = name;
    state = RoadState.CLOSE;
    time = 0;
  }

  public String getName(){
    return name;
  }

  public void set(int time, RoadState state){
    this.time = time;
    this.state = state;
  }

  public int getLeftoverCloseTime(int interval){
    if(state == RoadState.OPEN) return time;
    return time + interval;
  }
}


class QueueThread extends Thread{
  private Instant startTime;
  private int timePassed = 0;
  private ArrayList<Road> circularQueue;
  public int numberOfRoads;
  public int interval;

  public QueueThread(){
    super("QueueThread");
    startTime = startTime.now();
    circularQueue = new ArrayList<>();
  }

  public void doSystemMenu(){
    Main.menu.clearConsole();
    System.out.printf("! %ds. have passed since system startup !\n", timePassed);
    System.out.printf("! Number of roads: %d !\n", this.numberOfRoads);
    System.out.printf("! Interval: %d !\n", this.interval);
    printRoad();
    System.out.println("! Press \"Enter\" to open menu !");
  }

  public boolean addRoad(String name){
    if(circularQueue.size() == numberOfRoads){
      System.out.println("queue is full");
      return false;
    }
    Road road = new Road(name);
    if(circularQueue.isEmpty()){
      road.set(interval, Road.RoadState.OPEN);
      road.isActive = true;
      road.time = interval;
    }
    circularQueue.add(road);
    System.out.printf("%s Added!\n", name);
    return true;
  }

  public boolean deleteRoad() {
    if (circularQueue.isEmpty()) {
        System.out.println("queue is empty");
        return false;
    }
    Road road = circularQueue.remove(0);
    if(!circularQueue.isEmpty() && road.isActive){
      circularQueue.get(0).isActive = true;
    }
    System.out.printf("%s deleted!\n", road.getName());
    return true;
  }

  private int findActiveRoad(){
    if(circularQueue.isEmpty()) return -1;
    for (int i = 0; i < circularQueue.size(); i++) {
          if (circularQueue.get(i).isActive) {
              return i;
          }
      }
      return -1;
  }
  public int activateNextRoad(boolean changeStatus) {
        int activeRoadIndex = findActiveRoad();
        if (activeRoadIndex != -1) {
            circularQueue.get(activeRoadIndex).isActive = false;
            int nextRoadIndex = (activeRoadIndex + 1) % circularQueue.size();
            circularQueue.get(nextRoadIndex).isActive = true;
            if(changeStatus) {
              circularQueue.get(nextRoadIndex).set(interval, Road.RoadState.OPEN);
            }
            return nextRoadIndex;
        }
        return -1;
    }

  public void justRecalc(){
        int activeRoadIndex = findActiveRoad();
        if (activeRoadIndex != -1) {
          for(int i = 1; i < circularQueue.size(); i++){
            int index = i + activeRoadIndex;
            Road road = circularQueue.get(index % circularQueue.size());
            Road prevRoad = circularQueue.get((index - 1) % circularQueue.size());
            road.set(prevRoad.getLeftoverCloseTime(interval), Road.RoadState.CLOSE);
          }
        }
  }

  public void decreaseRoadCounter(){
        int activeRoadIndex = findActiveRoad();
        if (activeRoadIndex != -1) {
          if(circularQueue.get(activeRoadIndex).time == 1) {
            if(circularQueue.get(activeRoadIndex).state == Road.RoadState.CLOSE){
              circularQueue.get(activeRoadIndex).set(interval, Road.RoadState.OPEN);
            }else {
              activeRoadIndex = activateNextRoad(true);
            }
          }else{
            circularQueue.get(activeRoadIndex).time--;
          }
          for(int i = 1; i < circularQueue.size(); i++){
            int index = i + activeRoadIndex;
            Road road = circularQueue.get(index % circularQueue.size());
            Road prevRoad = circularQueue.get((index - 1) % circularQueue.size());
            road.set(prevRoad.getLeftoverCloseTime(interval), Road.RoadState.CLOSE);
          }
        }
  }
  public void printRoad(){
    System.out.println();
    for (Road road : circularQueue) {
      System.out.printf("%s will be %s for %ds.\u001B[0m\n",
              road.getName(), road.state == Road.RoadState.OPEN ? "\u001B[32mopen" : "\u001B[31mclosed", road.time);
    }
    System.out.println();
  }


  @Override
  public void run(){
    try {
//      System.out.println("thread startet");
      do {
        sleep(1000);
        timePassed++;
        justRecalc();
        if (Main.currentState == MenuState.SYSTEM) {
//          System.out.printf("State: %s\n", Main.currentState.name());
          //long duration = Duration.between(startTime, Instant.now()).getSeconds();
          doSystemMenu();
//          sleep(200);
        }
        decreaseRoadCounter();

//      } while (Main.currentState != MenuState.END);
      } while (!isInterrupted());
    }catch(Exception e) {
//      System.out.println("exception in Thread module");
    }
//    System.out.println("Thread endet");
  }

}

public class Main {
  public static MenuState currentState = MenuState.NOT_STARTED;
  public static Menu menu;

  public static void main(String[] args){
    Main.currentState = MenuState.MENU;

    int answer = -1;
    Scanner sc = new Scanner(System.in);
    try {
      menu = new Menu();
      QueueThread thread = new QueueThread();
      thread.numberOfRoads = menu.getNumberOfRoads();
      thread.interval = menu.getInterval();
      thread.start();

      do {
        answer = menu.doMenu();
        switch (answer) {
          case 1:
            System.out.print("Input road name: ");
            String name = sc.nextLine();
            thread.addRoad(name);
            sc.nextLine();
            break;
          case 2:
            thread.deleteRoad();
            sc.nextLine();
            break;
          case 3:
            //System.out.println("System opened");
            Main.currentState = MenuState.SYSTEM;
            sc.nextLine();
            Main.currentState = MenuState.MENU;
            break;
        }

      } while (answer != 0);
      Main.currentState = MenuState.END;
      thread.interrupt();
      thread.join();
    }catch(InterruptedException e) {
      System.out.println("Interrupted");
    }
    sc.close();
    System.out.println("Bye!");

  }
}
