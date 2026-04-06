package com.amhs.swim.test.testcase;

import com.amhs.swim.test.util.Logger;

/**
 * Lớp cơ sở cho tất cả các Test Case.
 * Cung cấp các phương thức chung để báo cáo kết quả.
 */
public abstract class BaseTestCase {
    protected String testCaseId;
    protected String testCaseName;

    public BaseTestCase(String id, String name) {
        this.testCaseId = id;
        this.testCaseName = name;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    /**
     * Phương thức chính để thực thi test case.
     * @return true nếu test pass, false nếu fail.
     */
    public abstract boolean execute() throws Exception;

    /**
     * Ghi log kết quả.
     * @param pass Trạng thái pass/fail.
     * @param message Thông báo chi tiết.
     */
    protected void reportResult(boolean pass, String message) {
        String status = pass ? "PASS" : "FAIL";
        Logger.log(status, testCaseId + " - " + testCaseName + ": " + message);
    }
}