package com.judge;

import com.judge.model.*;
import com.judge.service.CodeExecutionService;
import com.judge.service.OnlineCompilerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JudgeApplicationTests {

    @Autowired
    private CodeExecutionService executionService;

    @Autowired
    private OnlineCompilerService compilerService;

    @Test
    @DisplayName("Online Compiler: Should compile and execute Hello World successfully")
    void testOnlineCompilerHelloWorld() {
        String code = """
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Programiz!");
    }
}
""";
        CompilerRequest request = new CompilerRequest(code, "");
        CompilerResponse response = compilerService.execute(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getStdout().contains("Hello, Programiz!"));
        assertEquals(0, response.getExitCode());
    }

    @Test
    @DisplayName("Online Compiler: Should feed custom Stdin into Scanner correctly")
    void testOnlineCompilerWithStdin() {
        String code = """
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String name = sc.next();
        int age = sc.nextInt();
        System.out.println("User: " + name + ", Age: " + age);
    }
}
""";
        CompilerRequest request = new CompilerRequest(code, "Siddharth 22\n");
        CompilerResponse response = compilerService.execute(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getStdout().contains("User: Siddharth, Age: 22"));
    }

    @Test
    @DisplayName("Online Compiler: Should terminate infinite loops at 5.0s watchdog limit (destroyForcibly)")
    void testOnlineCompilerTle() {
        String infiniteLoop = """
public class Main {
    public static void main(String[] args) {
        while (true) {}
    }
}
""";
        CompilerRequest request = new CompilerRequest(infiniteLoop, "");
        long start = System.currentTimeMillis();
        CompilerResponse response = compilerService.execute(request);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertEquals("TIME_LIMIT_EXCEEDED", response.getStatus());
        assertTrue(elapsed >= 4800 && elapsed <= 8000, "Should terminate in ~5s, took: " + elapsed + "ms");
    }

    @Test
    @DisplayName("Online Compiler: Should return compilation errors with diagnostics")
    void testOnlineCompilerSyntaxError() {
        String brokenCode = """
public class Main {
    public static void main(String[] args) {
        int x = "incompatible types"
    }
}
""";
        CompilerRequest request = new CompilerRequest(brokenCode, "");
        CompilerResponse response = compilerService.execute(request);

        assertNotNull(response);
        assertEquals("COMPILATION_ERROR", response.getStatus());
        assertTrue(response.getStderr().contains("incompatible types") || response.getStderr().contains("error:"));
    }

    @Test
    @DisplayName("Online Judge: Two Sum should pass all test cases")
    void testTwoSumAccepted() {
        String code = """
import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        int[] result = twoSum(nums, target);
        System.out.println(result[0] + " " + result[1]);
    }

    public static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }
        return new int[] {-1, -1};
    }
}
""";
        ExecutionRequest request = new ExecutionRequest("two-sum", code, true);
        ExecutionResult result = executionService.execute(request);

        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getOverallVerdict());
        assertEquals(5, result.getTotalTests());
        assertEquals(5, result.getPassedTests());
    }

    @Test
    @DisplayName("Online Judge: Should evaluate arbitrary Custom Testcase without assertion error")
    void testJudgeCustomInput() {
        String code = """
import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNext()) return;
        String word = sc.next();
        System.out.println("Echo: " + word);
    }
}
""";
        ExecutionRequest request = new ExecutionRequest("two-sum", code, false, "HelloJudge");
        ExecutionResult result = executionService.execute(request);

        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getOverallVerdict());
        assertEquals(1, result.getTotalTests());
        assertEquals(1, result.getPassedTests());
        assertFalse(result.getTestCaseResults().isEmpty());
        assertTrue(result.getTestCaseResults().get(0).getActualOutput().contains("Echo: HelloJudge"));
    }

    @Test
    @DisplayName("Online Judge: Valid Parentheses should pass all test cases")
    void testValidParenthesesAccepted() {
        String code = """
import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNext()) return;
        String s = sc.next();
        System.out.println(isValid(s));
    }

    public static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(') stack.push(')');
            else if (c == '{') stack.push('}');
            else if (c == '[') stack.push(']');
            else if (stack.isEmpty() || stack.pop() != c) return false;
        }
        return stack.isEmpty();
    }
}
""";
        ExecutionRequest request = new ExecutionRequest("valid-parentheses", code, true);
        ExecutionResult result = executionService.execute(request);

        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getOverallVerdict());
        assertEquals(6, result.getTotalTests());
        assertEquals(6, result.getPassedTests());
    }

    @Test
    @DisplayName("C++ Compiler: Should compile and execute C++20 Hello World successfully")
    void testCppHelloWorld() {
        String cppCode = """
#include <iostream>

int main() {
    std::cout << "Hello from C++20!" << std::endl;
    return 0;
}
""";
        CompilerRequest request = new CompilerRequest(cppCode, "", "cpp");
        CompilerResponse response = compilerService.execute(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getStdout().contains("Hello from C++20!"));
    }

    @Test
    @DisplayName("C++ Online Judge: Two Sum in C++ should pass all test cases")
    void testCppTwoSumJudge() {
        String cppCode = """
#include <iostream>
#include <vector>
#include <unordered_map>

using namespace std;

class Solution {
public:
    static vector<int> twoSum(vector<int>& nums, int target) {
        unordered_map<int, int> map;
        for (int i = 0; i < nums.size(); i++) {
            int comp = target - nums[i];
            if (map.count(comp)) {
                return { map[comp], i };
            }
            map[nums[i]] = i;
        }
        return {-1, -1};
    }
};

int main() {
    int n;
    if (!(cin >> n)) return 0;
    vector<int> nums(n);
    for (int i = 0; i < n; i++) cin >> nums[i];
    int target;
    cin >> target;
    vector<int> res = Solution::twoSum(nums, target);
    cout << res[0] << " " << res[1] << endl;
    return 0;
}
""";
        ExecutionRequest request = new ExecutionRequest("two-sum", cppCode, true);
        request.setLanguage("cpp");
        ExecutionResult result = executionService.execute(request);

        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getOverallVerdict());
        assertEquals(5, result.getTotalTests());
        assertEquals(5, result.getPassedTests());
        assertTrue(result.getPercentileBeaten() >= 0);
    }

    @Test
    @DisplayName("Security Sandbox: Should block System.exit in Java")
    void testJavaSecurityViolation() {
        String maliciousJava = """
public class Main {
    public static void main(String[] args) {
        System.exit(0);
    }
}
""";
        CompilerRequest request = new CompilerRequest(maliciousJava, "", "java");
        CompilerResponse response = compilerService.execute(request);

        assertNotNull(response);
        assertEquals("SECURITY_VIOLATION", response.getStatus());
        assertTrue(response.getStderr().contains("System.exit"));
    }

    @Test
    @DisplayName("Security Sandbox: Should block system() in C++")
    void testCppSecurityViolation() {
        String maliciousCpp = """
#include <iostream>
#include <cstdlib>

int main() {
    system("calc.exe");
    return 0;
}
""";
        CompilerRequest request = new CompilerRequest(maliciousCpp, "", "cpp");
        CompilerResponse response = compilerService.execute(request);

        assertNotNull(response);
        assertEquals("SECURITY_VIOLATION", response.getStatus());
        assertTrue(response.getStderr().contains("system()"));
    }
}
