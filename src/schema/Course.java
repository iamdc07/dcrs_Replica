package schema;

import java.io.Serializable;
import java.util.ArrayList;

public class Course implements Serializable {
    private String term;
    private String course_id;
    private int capacity;
    private ArrayList<String> enrolledStudentId = new ArrayList<>();


    public int getCapacity() {
        return capacity;
    }

    public Course(int capacity, String course_ID, String term) {
        this.capacity = capacity;
        this.course_id = course_ID;
        this.term = term;
        this.enrolledStudentId.clear();
    }

    public String getCourse_ID() {
        return course_id;
    }

    public String getTerm() {
        return term;
    }

    public ArrayList<String> getEnrolledStudentId() {
        return enrolledStudentId;
    }

    public void setEnrolledStudentId(String enrolledStudentId) {
        this.enrolledStudentId.add(enrolledStudentId);
    }

    public void setEnrolledStudentIdList(ArrayList<String> enrolledStudentId) {
        this.enrolledStudentId = enrolledStudentId;
    }

    public boolean isCourseFull() {
        return this.enrolledStudentId.size() >= this.capacity;
    }
}
