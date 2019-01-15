package server;

import data.Data;
import schema.Course;
import schema.UdpPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class ServerOperations {
    private HashMap<String, HashMap<String, Course>> courseRecords = new HashMap<>();
    private HashMap<String, Integer> coursesAvailable = new HashMap<>();
    private List<String> advisorlist = new ArrayList<>();
    private HashMap<String, HashMap<String, ArrayList<String>>> studentlist = new HashMap<>();
    private UdpPacket udpPacket;
    private Logger logs;
    private String[] servers = new String[3];
    private int compPort, soenPort, insePort;
    protected static int flag = 0;

    protected ServerOperations(String courseCode, Logger logs) {
        super();
        this.logs = logs;
        for (Integer i = 0; i < 4; i++) {
            HashMap<String, ArrayList<String>> courses = new HashMap<>();
            String theStudentId = courseCode.concat("S").concat("100").concat(i.toString());
            this.studentlist.put(theStudentId, courses);
        }
        String theAdvisorId = courseCode.concat("A").concat("1001");
        this.advisorlist.add(theAdvisorId);
        servers[0] = "COMP";
        servers[1] = "SOEN";
        servers[2] = "INSE";
        compPort = 6789;
        soenPort = 6791;
        insePort = 6793;
    }

    public boolean advisor_exists(String advisor_id, String dept) {
        Boolean valid_advisor = false;
        for (String str : advisorlist) {
            if (str.equalsIgnoreCase(advisor_id)) {
                valid_advisor = true;
            }
        }
        return valid_advisor;
    }

    public boolean student_exists(String student_id, String dept) {
        Boolean valid_student = false;
        for (Map.Entry<String, HashMap<String, ArrayList<String>>> student : this.studentlist.entrySet()) {
            String id = student.getKey();
            if (id.equalsIgnoreCase(student_id)) {
                valid_student = true;
            }
        }
        return valid_student;
    }

    public String enroll(String courseId, String studentId, String term, String dept, boolean swapOp) {
        HashMap<String, Course> theTerm = this.courseRecords.get(term);
        HashMap<String, ArrayList<String>> courses = this.studentlist.get(studentId);
        String idPrefix = studentId.substring(0, 4);


        if (courseId.substring(0, 4).equals(dept)) {
            if (theTerm != null && theTerm.containsKey(courseId)) {
                Course course = theTerm.get(courseId);

                String checkValue = enrollChecks(courseId, course, studentId, term, courses, swapOp);

                switch (checkValue) {
                    case "No seats available for the course":
                        return checkValue;
                    case "You have enrolled in 3 courses in this term":
                        return checkValue;
                    case "you are already enrolled in the course":
                        return checkValue;
                    case "No errors":
                        break;
                }

                // Enroll for the course
                course.setEnrolledStudentId(studentId);
                theTerm.put(courseId, course);
                this.courseRecords.put(term, theTerm);

                if (idPrefix.equals(dept)) {
                    System.out.println(term);
                    if (this.studentlist.containsKey(studentId)) {

                        if (courses.containsKey(term)) {
                            ArrayList<String> termCourses = courses.get(term);
                            termCourses.add(courseId);
                            courses.put(term, termCourses);
                            this.studentlist.put(studentId, courses);

                            for (Map.Entry<String, HashMap<String, ArrayList<String>>> thestudentid : this.studentlist.entrySet()) {
                                String studentid1 = thestudentid.getKey();
                                for (Map.Entry<String, ArrayList<String>> theterm : thestudentid.getValue().entrySet()) {
                                    String str = theterm.getKey();
                                    ArrayList<String> courses1 = theterm.getValue();
                                    System.out.print(studentid1 + " ");
                                    System.out.println(str);
                                    for (String course1 : courses1) {
                                        System.out.println(course1);
                                    }
                                }
                            }
                            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                                    + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                                    + " | Request Succesfully Completed" + " | Server Response: Successfully enrolled!");
                            return "You have successfully enrolled in the course";
                        } else {
                            ArrayList<String> termCourses = new ArrayList<>();
                            termCourses.add(courseId);
                            courses.put(term, termCourses);
                            this.studentlist.put(studentId, courses);
                            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                                    + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                                    + " | Request Succesfully Completed" + " | Server Response: Successfully enrolled!");
                            return "You have successfully enrolled in the course";
                        }
                    }
                } else {
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                            + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                            + " | Request Succesfully Completed" + " | Server Response: Successfully enrolled!");
                    return "You have successfully enrolled in the course";
                }
            } else {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                        + " | Request Parameters: " + studentId + ", " + courseId + ", " + term + " | Request Failed"
                        + " | Server Response: Could not find the course!");
                return "Course is not available";
            }
        } else if (!(courseId.substring(0, 4).equals(idPrefix))) { // Check for Udp call
            String checkMessage = new String();

            if (courses != null && courses.containsKey(term)) {
                List<String> termCourses = courses.get(term);
                checkMessage = udpEnrollChecks(courseId, studentId, term, dept, false, true);
            }

            switch (checkMessage) {
                case "You have enrolled in 3 courses in this term":
                    return checkMessage;
                case "you are already enrolled in the course":
                    return checkMessage;
                case "You have enrolled in 2 other department courses":
                    return checkMessage;
                case "Course is not available":
                    return checkMessage;
                case "No errors":
                    break;
            }

            // Code for enrolling in other department

            System.out.println("Check in prefix");
            System.out.println("Course prefix: " + courseId.substring(0, 4) + " | Studentid: " + studentId + " | term: "
                    + term + " | courseId: " + courseId);
            udpPacket = new UdpPacket(1, courseId, studentId, term, courseId.substring(0, 4), false);
            String response = (String) udpCall(courseId.substring(0, 4));
            System.out.println("SERVER response:" + response);
            if (response.equalsIgnoreCase("Successfully enrolled!")) {
                HashMap<String, ArrayList<String>> studentCourses = this.studentlist.get(studentId);
                ArrayList<String> termCourses = studentCourses.get(term);
                if (termCourses != null) {
                    termCourses.add(courseId);
                    studentCourses.put(term, termCourses);
                    this.studentlist.put(studentId, studentCourses);
                } else {
                    ArrayList<String> termCourses1 = new ArrayList<>();
                    termCourses1.add(courseId);
                    studentCourses.put(term, termCourses1);
                    this.studentlist.put(studentId, studentCourses);
                }

                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                        + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                        + " | Request Succesfully Completed" + " | Server Response: " + response);
                return response;
            } else {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                        + " | Request Parameters: " + studentId + ", " + courseId + ", " + term + " | Request Failed"
                        + " | Server Response: " + response);
                return response;
            }
        } else {
            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                    + " | Request Parameters: " + studentId + ", " + courseId + ", " + term + " | Request Failed"
                    + " | Server Response: Could not find the course!");
            return "Course is not available";
        }
        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course" + " | Request Parameters: "
                + studentId + ", " + courseId + ", " + term + " | Request Failed"
                + " | Server Response: Term not found!");
        return "Course is not available";
    }

    public String addCourse(String advisor_id, String course_id, String term, String dept, int capacity) {
        if (courseRecords.containsKey(term)) {
            HashMap<String, Course> theTerm = courseRecords.get(term);
            if (theTerm.containsKey(course_id)) {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Add Course"
                        + " | Request Parameters: " + advisor_id + ", " + course_id + ", " + term + " | Request Failed"
                        + " | Server Response: Course already exists! Try for other term.");
                return "Course is already added";
            } else {
                Course course = new Course(capacity, course_id, term);
                theTerm.put(course_id, course);
                courseRecords.put(term, theTerm);
            }
        } else {
            HashMap<String, Course> c = new HashMap<>();
            Course course = new Course(capacity, course_id, term);
            c.put(course.getCourse_ID(), course);
            courseRecords.put(course.getTerm(), c);
        }
        display_courses();
        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Add Course" + " | Request Parameters: "
                + advisor_id + ", " + course_id + ", " + term + " | Request Completed"
                + " | Server Response: Course Successfully Added");
        return "You have successfully added the course";
    }

    public String dropCourse(String student_id, String courseId, String term, String dept) {
        if (courseId.substring(0, 4).equalsIgnoreCase(dept)) {
            if (courseRecords.containsKey(term)) {
                HashMap<String, Course> theTerm = this.courseRecords.get(term);
                String idPrefix = student_id.substring(0, 4);
                System.out.println("Course id equals dept:" + courseId.substring(0, 4).equals(dept));
                if (theTerm.containsKey(courseId)) {
                    Course course = theTerm.get(courseId);
                    ArrayList<String> theEnrolled = course.getEnrolledStudentId();

                    boolean result = theEnrolled.remove(student_id);

                    if (result) {
                        course.setEnrolledStudentIdList(theEnrolled);
                        theTerm.put(courseId, course);
                        this.courseRecords.put(term, theTerm);
                    } else {
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Drop Course"
                                + " | Request Parameters: " + student_id + ", " + courseId + ", " + term
                                + " | Request Failed " + " | Server Response: Student not enrolled");
                        return "You are not enrolled in the course";
                    }

                    if (idPrefix.equals(dept)) {
                        HashMap<String, ArrayList<String>> studentCourses = this.studentlist.get(student_id);
                        ArrayList<String> termCourses = studentCourses.get(term);
                        termCourses.remove(courseId);
                        studentCourses.put(term, termCourses);
                        this.studentlist.put(student_id, studentCourses);
                    }
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Drop Course"
                            + " | Request Parameters: " + student_id + ", " + courseId + ", " + term
                            + " | Request Completed " + " | Server Response: Course Dropped");
                    return "you are successfully dropped the course";
                }
            }
        } else if (!(courseId.substring(0, 4).equals(dept))) {
            udpPacket = new UdpPacket(4, courseId, student_id, term, courseId.substring(0, 4), true);
            String response = (String) udpCall(courseId.substring(0, 4));
//					System.out.println("Response from Udp call" + response);
            if (response.equalsIgnoreCase("Course Dropped")) {
                HashMap<String, ArrayList<String>> studentCourses = this.studentlist.get(student_id);
                ArrayList<String> termCourses = studentCourses.get(term);
                termCourses.remove(courseId);
                studentCourses.put(term, termCourses);
                this.studentlist.put(student_id, studentCourses);

                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Drop Course" + " | Request Parameters: "
                        + student_id + ", " + courseId + ", " + term + " | Request Completed" + " | Server Response: "
                        + response);
                return response;
            } else if (response.equalsIgnoreCase("Fail")) {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Drop Course" + " | Request Parameters: "
                        + student_id + ", " + courseId + ", " + term + " | Request Completed" + " | Server Response: Not enrolled in the Course");
                return "You are not enrolled in the course";
            }

        }
        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Drop Course" + " | Request Parameters: "
                + student_id + ", " + courseId + ", " + term + " | Request Failed"
                + " | Server Response: Could not find the course.");
        return "You are not enrolled in the course";
    }

    public String removeCourse(String id, String course_id, String term, String dept) {
        String[] departments = new String[2];

        if (courseRecords.containsKey(term)) {
            HashMap<String, Course> courseMap = courseRecords.get(term);
            if (courseMap.containsKey(course_id)) {
                if (course_id.substring(0, 4).equalsIgnoreCase(dept)) {
                    courseMap.remove(course_id);
                    this.courseRecords.put(term, courseMap);

                    if (dept.equalsIgnoreCase("COMP")) {
                        departments[0] = "SOEN";
                        departments[1] = "INSE";
                    } else if (dept.equalsIgnoreCase("SOEN")) {
                        departments[0] = "COMP";
                        departments[1] = "INSE";
                    } else if (dept.equalsIgnoreCase("INSE")) {
                        departments[0] = "COMP";
                        departments[1] = "SOEN";
                    }

                    removeCourseUdp(course_id, term);

                    udpPacket = new UdpPacket(5, course_id, id, term, departments[0], false);
                    String response = (String) udpCall(departments[0]);
                    System.out.println("RESPONSE1: " + response);

                    udpPacket = new UdpPacket(5, course_id, id, term, departments[1], false);
                    response = (String) udpCall(departments[1]);
                    System.out.println("RESPONSE2: " + response);
                }

                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Remove Course"
                        + " | Request Parameters: " + id + ", " + course_id + ", " + term + " | Request Completed"
                        + " | Server Response: Course Successfully Removed");
                return "You have successfully remove the course";
            } else {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Remove Course"
                        + " | Request Parameters: " + id + ", " + course_id + ", " + term + " | Request Failed"
                        + " | Server Response: Course doesn't exist!");
                return "Course id not exist";
            }
        } else {
            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Remove Course" + " | Request Parameters: "
                    + id + ", " + course_id + ", " + term + " | Request Failed"
                    + " | Server Response: Term doesn't exist");
            return "Course id not exist";
        }
    }

    public String listCourseAvailability(String advisor_id, String term, String dept) {
        HashMap<String, Course> theTerm = courseRecords.get(term);
        HashMap<String, Integer> courses = new HashMap<>();
        ArrayList<Short> seatsArray = new ArrayList<>();
        ArrayList<String> courseArray = new ArrayList<>();
        ArrayList<String> mainResponse = new ArrayList<>();
        String[] allValue;
        String courseSeats = "";
        int counter = 0;

        if (theTerm != null) {
            for (Map.Entry<String, Course> course : theTerm.entrySet()) {
                Course courseObj = course.getValue();
                int seatsAvailable = courseObj.getCapacity() - courseObj.getEnrolledStudentId().size();
                courseArray.add(course.getKey());
                seatsArray.add((short) seatsAvailable);
                courses.put(course.getKey(), seatsAvailable);
            }

            for (String item : courseArray) {
                courseSeats = courseSeats.concat(item + ";");
            }
            counter = 0;
            courseSeats = courseSeats.concat(",");
            for (short item : seatsArray) {
                courseSeats = courseSeats.concat(String.valueOf(item) + ";");
            }
            System.out.println(courseSeats);
            mainResponse.add(courseSeats);
        }
        System.out.println(dept);
        ArrayList<Integer> ports = new ArrayList<>();
        ArrayList<String> departments = new ArrayList<>();

        if (dept.equalsIgnoreCase("COMP")) {
            ports.add(soenPort + 1);
            departments.add("SOEN");
            ports.add(insePort + 1);
            departments.add("INSE");
        } else if (dept.equalsIgnoreCase("SOEN")) {
            ports.add(compPort + 1);
            departments.add("COMP");
            ports.add(insePort + 1);
            departments.add("INSE");
        } else if (dept.equalsIgnoreCase("INSE")) {
            ports.add(soenPort + 1);
            departments.add("SOEN");
            ports.add(compPort + 1);
            departments.add("COMP");
        }

        System.out.println(ports.get(0));
        System.out.println(ports.get(1));
        System.out.println(departments.get(1));

        udpPacket = new UdpPacket(3, "", advisor_id, term, departments.get(0), false);
        @SuppressWarnings("unchecked")
        String response = (String) udpCall(departments.get(0));

        udpPacket = new UdpPacket(3, "", advisor_id, term, departments.get(1), false);
        @SuppressWarnings("unchecked")
        String response1 = (String) udpCall(departments.get(1));

        if (response != null && !(response.equalsIgnoreCase("No Term Found")) && !(response.equalsIgnoreCase("No Term Found"))) {
            mainResponse.add(response);
        }
        if (response1 != null && !(response1.equalsIgnoreCase("No Term Found"))) {
            mainResponse.add(response1);
        }
        String result = response + response1 + courses.toString();

        counter = 0;
//        if (response != null || response1 != null || !(response1.equalsIgnoreCase("No Term Found")) || !(response.equalsIgnoreCase("error in server"))) {
//            for (String item : mainResponse) {
//                result[counter++] = item;
//            }
//        }

        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: List Course Availability"
                + " | Request Parameters: " + advisor_id + ", " + term + " | Request Completed"
                + " | Server Response: " + result);
        return result;
    }

    public String getClassSchedule(String studentId) {
        HashMap<String, ArrayList<String>> courseMap = this.studentlist.get(studentId);
        String course = "";

        if (flag == 0)
            return "You are not enrolled in any course";


        if (courseMap == null) {
            return "You are not enrolled in any course";
        } else if (!(courseMap.isEmpty())) {
            for (Map.Entry<String, ArrayList<String>> theTerm : courseMap.entrySet()) {
                String termName = theTerm.getKey();
                List<String> coursesList = theTerm.getValue();
                course = course.concat(termName + ";");

                for (String theCourse : coursesList) {
                    course = course.concat(theCourse + ";");
                }
                course = course.concat(",");
            }

            System.out.println("COURSE STRING: " + course);

            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Get Class Schedule"
                    + " | Request Parameters: " + studentId + " | Request Completed" + " | Server Response: " + course);
            return course;
        } else {
            course = "";
            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Get Class Schedule"
                    + " | Request Parameters: " + studentId + " | Request Failed" + " | Server Response: " + course);
            return course;
        }

    }

    public synchronized String swapCourse(String studentId, String oldCourseId, String newCourseId, String dept, String term) {
        String oldCourseIdPrefix = oldCourseId.substring(0, 4);
        String newCourseIdPrefix = newCourseId.substring(0, 4);
        String studentIdPrefix = studentId.substring(0, 4);
        boolean udpCall = false, checkCrossEnroll = false, swapOp = false;

        if (!(studentIdPrefix.equalsIgnoreCase(newCourseIdPrefix)) || (!(newCourseIdPrefix.equalsIgnoreCase(oldCourseIdPrefix)))) {
            udpCall = true;
        }

        if (oldCourseIdPrefix.equalsIgnoreCase(dept)) {
            checkCrossEnroll = true;
        }

        if (studentIdPrefix.equalsIgnoreCase(newCourseIdPrefix)) {
            swapOp = true;
        }

        String checkUdpValue = udpEnrollChecks(newCourseId, studentId, term, dept, true, checkCrossEnroll);
        System.out.println("CheckUdpValue: " + checkUdpValue);

        switch (checkUdpValue) {
            case "You have enrolled in 2 other department courses":
                return checkUdpValue;
            case "You have enrolled in 3 courses in this term":
                return checkUdpValue;
            case "you are already enrolled in the course":
                return checkUdpValue;
            case "Course is not available":
                return checkUdpValue;
            case "No errors":
                break;
        }


        if (udpCall) { // Udp Call
            udpPacket = new UdpPacket(1, newCourseId, studentId, term, newCourseId.substring(0, 4), swapOp);
            String response = (String) udpCall(newCourseId.substring(0, 4));

            if (response.equalsIgnoreCase("You have successfully enrolled in the course")) {
                if (!(studentIdPrefix.equalsIgnoreCase(newCourseIdPrefix))) {
                    enrollToStudentList(studentId, newCourseId, term);
                }
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                        + studentId + ", " + newCourseId + ", " + term + " | Request Completed" + " | Server Response: Successfully enrolled into new Course");
                udpPacket = new UdpPacket(4, oldCourseId, studentId, term, oldCourseId.substring(0, 4), swapOp);
                String dropResponse = (String) udpCall(oldCourseId.substring(0, 4));

                if (dropResponse.equalsIgnoreCase("you are successfully dropped the course")) {
                    if (!(studentIdPrefix.equalsIgnoreCase(oldCourseIdPrefix))) {
                        dropFromStudentList(studentId, oldCourseId, term);
                    }
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                            + studentId + ", " + oldCourseId + ", " + term + " | Request Completed" + " | Server Response: Course Dropped");
                    return "Swap done successfully";
                } else if (dropResponse.equalsIgnoreCase("You are not enrolled in the course")) {
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                            + studentId + ", " + oldCourseId + ", " + term + " | Request Failed" + " | Server Response: Student not enrolled in the course");
                    udpPacket = new UdpPacket(4, newCourseId, studentId, term, newCourseId.substring(0, 4), swapOp);
                    String dropBack = (String) udpCall(newCourseId.substring(0, 4));

                    if (dropBack.equalsIgnoreCase("you are successfully dropped the course")) {
                        if (!(studentIdPrefix.equalsIgnoreCase(newCourseIdPrefix))) {
                            dropFromStudentList(studentId, newCourseId, term);
                        }
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                + studentId + ", " + newCourseId + ", " + term + " | Request Failed" + " | Server Response: Dropped the newly enrolled course");
                        return ("You are not enrolled in the old course");
                    }
                } else if (dropResponse.equalsIgnoreCase("You are not enrolled in the course")) {
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                            + studentId + ", " + oldCourseId + ", " + term + " | Request Failed" + " | Server Response: Could not find the old course");
                    udpPacket = new UdpPacket(4, newCourseId, studentId, term, newCourseId.substring(0, 4), swapOp);
                    String dropBack = (String) udpCall(newCourseId.substring(0, 4));

                    if (dropBack.equalsIgnoreCase("you are successfully dropped the course")) {
                        if (!(studentIdPrefix.equalsIgnoreCase(newCourseIdPrefix))) {
                            dropFromStudentList(studentId, newCourseId, term);
                        }
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                + studentId + ", " + newCourseId + ", " + term + " | Request Failed" + " | Server Response: Dropped the newly enrolled course");
                        return ("You are not enrolled in the old course");
                    }
                }
            } else if (response.equalsIgnoreCase("Course is not available")) {
                return ("Course is not available");
            } else if (response.equalsIgnoreCase("Course is not available")) {
                return ("Course is not available");
            }
        } else {
            HashMap<String, Course> theTerm = this.courseRecords.get(term);
            HashMap<String, ArrayList<String>> courses = this.studentlist.get(studentId);

            if (theTerm != null && theTerm.containsKey(newCourseId)) {
                Course course = theTerm.get(newCourseId);

                String checkValue = enrollChecks(newCourseId, course, studentId, term, courses, true);

                switch (checkValue) {
                    case "No seats available for the course":
                        return checkValue;
                    case "You have enrolled in 3 courses in this term":
                        return checkValue;
                    case "you are already enrolled in the course":
                        return checkValue;
                    case "No errors":
                        break;
                }

                String dropResult = dropCourse(studentId, oldCourseId, term, dept);

                if (dropResult.equalsIgnoreCase("you are successfully dropped the course")) {
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                            + studentId + ", " + oldCourseId + ", " + term + " | Drop Request Completed" + " | Server Response: Course found and Dropped");
                    String enrollResult = enroll(newCourseId, studentId, term, dept, swapOp);

                    if (enrollResult.equalsIgnoreCase("You have successfully enrolled in the course")) {
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                + studentId + ", " + newCourseId + ", " + term + " | Request Completed" + " | Server Response: Successfully enrolled");
                        return "Swap done successfully";
                    } else if (enrollResult.equalsIgnoreCase("Course is not available")) {
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                + studentId + ", " + newCourseId + ", " + term + " | Request Failed" + " | Server Response: Course not found");
                        String enrollBack = enroll(studentId, oldCourseId, term, dept, swapOp);

                        if (enrollBack.equalsIgnoreCase("You have successfully enrolled in the course")) {
                            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                    + studentId + ", " + oldCourseId + ", " + term + " | Request Failed" + " | Server Response: Reverted Drop Operation");
                            return ("Course is not available");
                        }
                    } else if (enrollResult.equalsIgnoreCase("Course is not available")) {
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                + studentId + ", " + newCourseId + ", " + term + " | Request Failed" + " | Server Response: Term not found");
                        String enrollBack = enroll(studentId, oldCourseId, term, dept, swapOp);

                        if (enrollBack.equalsIgnoreCase("You have successfully enrolled in the course")) {
                            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                                    + studentId + ", " + oldCourseId + ", " + term + " | Request Failed" + " | Server Response: Reverted Drop Operation");
                            return ("Course is not available");
                        }
                    }
                } else if (dropResult.equalsIgnoreCase("You are not enrolled in the course")) {
                    return ("You are not enrolled in the course");
                } else if (dropResult.equalsIgnoreCase("You are not enrolled in the course")) {
                    return "You are not enrolled in the course";
                }
            } else {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Swap Course" + " | Request Parameters: "
                        + studentId + ", " + newCourseId + ", " + term + " | Request Failed" + " | Server Response: Course not found");
                return "No seats available for the course";
            }
        }
        return "Server Error";
    }

    private void enrollToStudentList(String studentId, String courseId, String term) {
        HashMap<String, ArrayList<String>> studentCourses = this.studentlist.get(studentId);
        ArrayList<String> termCourses = studentCourses.get(term);
        if (termCourses != null) {
            termCourses.add(courseId);
            studentCourses.put(term, termCourses);
            this.studentlist.put(studentId, studentCourses);
        } else {
            ArrayList<String> termCourses1 = new ArrayList<>();
            termCourses1.add(courseId);
            studentCourses.put(term, termCourses1);
            this.studentlist.put(studentId, studentCourses);
        }
    }

    private void dropFromStudentList(String student_id, String courseId, String term) {
        HashMap<String, ArrayList<String>> studentCourses = this.studentlist.get(student_id);
        ArrayList<String> termCourses = studentCourses.get(term);
        termCourses.remove(courseId);
        studentCourses.put(term, termCourses);
        this.studentlist.put(student_id, studentCourses);
    }

    protected String enrollChecks(String courseId, Course course, String studentId, String term, HashMap<String, ArrayList<String>> courses, boolean swapOp) {
        String idPrefix = studentId.substring(0, 4);
        if (course.isCourseFull()) {
            logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                    + " | Request Parameters: " + studentId + ", " + courseId + ", " + term + " | Request Failed"
                    + " | Server Response: The course is full!");
            return "No seats available for the course";
        }

        if (courseId.substring(0, 4).equals(idPrefix)) {
            if (courses.containsKey(term)) {
                List<String> termCourses = courses.get(term);
                if (termCourses.size() == 3 && !swapOp) {
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                            + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                            + " | Request Failed"
                            + " | Server Response: You have already reached your limit for this term!");
                    return "You have enrolled in 3 courses in this term";
                }
                for (String str : termCourses) {
                    if (str.equals(courseId)) {
                        logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                                + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                                + " | Request Failed"
                                + " | Server Response: You have already enrolled for this course!");
                        return "you are already enrolled in the course";
                    }
                }
            }
        }
        return "No errors";
    }

    protected String udpEnrollChecks(String courseId, String studentId, String term, String dept, boolean swapOp, boolean checkCrossEnroll) {
        HashMap<String, ArrayList<String>> courses = this.studentlist.get(studentId);

        List<String> termCourses = courses.get(term);
        int crossEnrollLimit = 0;
        if (courses != null && termCourses != null) {
            if (termCourses.size() == 3 && !swapOp) {
                logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                        + " | Request Parameters: " + studentId + ", " + courseId + ", " + term
                        + " | Request Failed"
                        + " | Server Response: You have already reached your limit for this term!");
                return "You have enrolled in 3 courses in this term";
            }

            for (String str : termCourses) {
                if (!(str.substring(0, 4).equalsIgnoreCase(dept))) {
                    crossEnrollLimit++;
                    if (str.equals(courseId)) {
                        return "you are already enrolled in the course";
                    }
                }
                if (str.equals(courseId)) {
                    return "you are already enrolled in the course";
                }
            }
            if (crossEnrollLimit == 2) {
                if (checkCrossEnroll) {
                    logs.info("Date & Time: " + LocalDateTime.now() + " | Request type: Enroll Course"
                            + " | Request Parameters: " + studentId + ", " + courseId + ", " + term + " | Request Failed"
                            + " | Server Response: You cannot enroll for more than 2 courses from other Department!");
                    return "You have enrolled in 2 other department courses";
                }
            }
            return "No errors";
        } else {
            return "Course is not available"; // check this response message
        }
    }

    private Object udpCall(String dept) {
        try {
            int port = 0;

            if (dept.equalsIgnoreCase("COMP")) {
                port = compPort;
            } else if (dept.equalsIgnoreCase("SOEN")) {
                port = soenPort;
            } else if (dept.equalsIgnoreCase("INSE")) {
                port = insePort;
            }

            Object response;
            DatagramSocket socket = new DatagramSocket();

            byte[] requestMessage = serialize(udpPacket);
            DatagramPacket outgoingPacket;
            outgoingPacket = new DatagramPacket(requestMessage, requestMessage.length,
                    InetAddress.getByName("localhost"), port);
            socket.send(outgoingPacket);

            // Incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);

            response = (String) deserialize(incomingPacket.getData());

            return response;

        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from server.\nMessage: " + e.getMessage());
        }
        return "error in server";
    }

    protected String removeCourseUdp(String course_id, String term) {
        for (Map.Entry<String, HashMap<String, ArrayList<String>>> theTerm : this.studentlist.entrySet()) {
            for (Map.Entry<String, ArrayList<String>> courses : theTerm.getValue().entrySet()) {
                String eachTerm = courses.getKey();
                System.out.println("Each term: " + eachTerm);
                System.out.println("Term: " + term);
                if (eachTerm.equalsIgnoreCase(term)) {
                    List<String> coursesList = courses.getValue();
                    coursesList.remove(course_id);
                }
            }
        }
        return "Removed Successfullly";
    }

    protected String listCourseAvailabilityUdp(String advisor_id, String term, String dept) {
        HashMap<String, Integer> courses = new HashMap<>();
        HashMap<String, Course> theTerm = this.courseRecords.get(term);
        ArrayList<Short> seatsArray = new ArrayList<>();
        ArrayList<String> courseArray = new ArrayList<>();
        String courseSeats = "";
        int counter = 0;

        if (theTerm != null) {
            for (Map.Entry<String, Course> course : theTerm.entrySet()) {
                Course courseObj = course.getValue();
                int seatsAvailable = courseObj.getCapacity() - courseObj.getEnrolledStudentId().size();
                courseArray.add(course.getKey());
                seatsArray.add((short) seatsAvailable);
                courses.put(course.getKey(), seatsAvailable);
            }

            for (String item : courseArray) {
                courseSeats = courseSeats.concat(item + ";");
            }
            counter = 0;
            courseSeats = courseSeats.concat(",");
            for (short item : seatsArray) {
                courseSeats = courseSeats.concat(String.valueOf(item) + ";");
            }
            return courses.toString();
        } else {
            return "No Term Found";
        }
    }

    private void display_courses() {
        for (Map.Entry<String, HashMap<String, Course>> term : this.courseRecords.entrySet()) {
            String termName = term.getKey();
            for (Map.Entry<String, Course> course : term.getValue().entrySet()) {
                String courseId = course.getKey();
                System.out.println("termName: " + termName + " courseId: " + courseId);
            }
        }
    }

    public Data sendData(){
        HashMap<String, HashMap<String,Integer>> records = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Course>> term : this.courseRecords.entrySet()) {
            String termName = term.getKey();
            HashMap<String, Integer> innerMap = new HashMap<>();
            for (Map.Entry<String, Course> course : term.getValue().entrySet()) {
                String courseId = course.getKey();
                innerMap.put(courseId, course.getValue().getCapacity());
                System.out.println("termName: " + termName + " courseId: " + courseId);
            }
            records.put(termName, innerMap);
        }
        Data data = new Data(records, studentlist, 10);
        return data;
    }

    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return o.readObject();
            }
        }
    }
}
