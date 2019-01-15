package schema;

import java.io.Serializable;

public class UdpPacket implements Serializable {
    //	private static final long serialVersionUID = 1L;
    public int operationNumber;
    public String studentId, term, dept, courseId;
    public boolean checkUdpEnroll, swapOp;

    public UdpPacket(int Number, String courseId, String studentId, String term, String dept, boolean swapOp) {
        this.operationNumber = Number;
        this.studentId = studentId;
        this.term = term;
        this.dept = dept;
        this.courseId = courseId;
        this.swapOp = swapOp;
    }
}