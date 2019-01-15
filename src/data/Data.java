package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Data implements Serializable {
    public String courseID;
    public String semester;
    public Integer capacity;
    public String userID;
    public String studentID;
    public String oldCourseID;
    public String newCourseID;
    public Integer operationID;
    public Integer sequenceID;
    public String department;
    public String error;
    public String replicaNumber;
    public boolean ack;
    public HashMap<String, HashMap<String, Integer>> courseList;
    public HashMap<String, HashMap<String, ArrayList<String>>> studentList;


    public Data(String courseID, String semester, Integer capacity, String userID, String studentID, String oldCourseID, String newCourseID, Integer operationID, Integer sequenceID, String department, String error, String replicaNumber, boolean ack) {
        this.courseID = courseID;
        this.semester = semester;
        this.capacity = capacity;
        this.userID = userID;
        this.studentID = studentID;
        this.oldCourseID = oldCourseID;
        this.newCourseID = newCourseID;
        this.operationID = operationID;
        this.sequenceID = sequenceID;
        this.department = department;
        this.error = error;
        this.replicaNumber = replicaNumber;
        this.ack = ack;
    }

    public Data(HashMap<String, HashMap<String, Integer>> courseList, HashMap<String, HashMap<String, ArrayList<String>>> studentList, int operationID) {
        this.courseID = "";
        this.semester = "";
        this.capacity = 0;
        this.userID = "";
        this.studentID = "";
        this.oldCourseID = "";
        this.newCourseID = "";
        this.operationID = operationID;
        this.sequenceID = 0;
        this.department = "";
        this.error = "";
        this.courseList = courseList;
        this.studentList = studentList;
    }

    public Data(String error, int operationID){
        this.error = error;
        this.courseID = "";
        this.semester = "";
        this.capacity = 0;
        this.userID = "";
        this.studentID = "";
        this.oldCourseID = "";
        this.newCourseID = "";
        this.operationID = operationID;
        this.sequenceID = 0;
        this.department = "";
        this.error = error;
    }

}
