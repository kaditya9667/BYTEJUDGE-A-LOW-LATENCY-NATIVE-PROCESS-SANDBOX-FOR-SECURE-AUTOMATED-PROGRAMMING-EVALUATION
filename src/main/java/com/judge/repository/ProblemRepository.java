package com.judge.repository;

import com.judge.model.Problem;
import com.judge.model.TestCase;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository containing pre-loaded DSA problems with Java and C++ starter code.
 */
@Repository
public class ProblemRepository {

    private final Map<String, Problem> problems = new ConcurrentHashMap<>();

    public ProblemRepository() {
        initProblems();
    }

    private void initProblems() {
        // Problem 1: Two Sum
        List<String> twoSumExamples = List.of(
                "Input:\n4\n2 7 11 15\n9\nOutput:\n0 1\nExplanation: nums[0] + nums[1] == 9, so return indices 0 1.",
                "Input:\n3\n3 2 4\n6\nOutput:\n1 2\nExplanation: nums[1] + nums[2] == 6, so return indices 1 2."
        );
        List<String> twoSumConstraints = List.of(
                "2 <= n <= 10^4",
                "-10^9 <= nums[i] <= 10^9",
                "-10^9 <= target <= 10^9",
                "Exactly one valid answer exists."
        );
        String twoSumStarterCode = """
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
        String twoSumStarterCodeCpp = """
#include <iostream>
#include <vector>
#include <unordered_map>

using namespace std;

class Solution {
public:
    static vector<int> twoSum(vector<int>& nums, int target) {
        unordered_map<int, int> map;
        for (int i = 0; i < nums.size(); i++) {
            int complement = target - nums[i];
            if (map.count(complement)) {
                return { map[complement], i };
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
    vector<int> result = Solution::twoSum(nums, target);
    cout << result[0] << " " << result[1] << endl;
    return 0;
}
""";
        List<TestCase> twoSumTests = List.of(
                new TestCase("4\n2 7 11 15\n9", "0 1", false),
                new TestCase("3\n3 2 4\n6", "1 2", false),
                new TestCase("2\n3 3\n6", "0 1", false),
                new TestCase("5\n1 5 8 19 25\n33", "2 4", true),
                new TestCase("6\n-1 -2 -3 -4 -5 9\n-6", "1 3", true)
        );
        Problem p1 = new Problem(
                "two-sum",
                "Two Sum",
                "Easy",
                "Given an array of integers `nums` and an integer `target`, return the indices of the two numbers such that they add up to `target`.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice. Output the two 0-based indices separated by a space in ascending order.",
                twoSumExamples,
                twoSumConstraints,
                twoSumStarterCode,
                twoSumStarterCodeCpp,
                twoSumTests
        );
        problems.put(p1.getId(), p1);

        // Problem 2: Valid Parentheses
        List<String> validParenthesesExamples = List.of(
                "Input:\n()\nOutput:\ntrue",
                "Input:\n()[]{}\nOutput:\ntrue",
                "Input:\n(]\nOutput:\nfalse"
        );
        List<String> validParenthesesConstraints = List.of(
                "1 <= s.length <= 10^4",
                "s consists of parentheses only: '()[]{}'."
        );
        String validParenthesesStarterCode = """
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
        String validParenthesesStarterCodeCpp = """
#include <iostream>
#include <string>
#include <stack>

using namespace std;

class Solution {
public:
    static bool isValid(string s) {
        stack<char> st;
        for (char c : s) {
            if (c == '(') st.push(')');
            else if (c == '{') st.push('}');
            else if (c == '[') st.push(']');
            else if (st.empty() || st.top() != c) return false;
            else st.pop();
        }
        return st.empty();
    }
};

int main() {
    string s;
    if (!(cin >> s)) return 0;
    cout << (Solution::isValid(s) ? "true" : "false") << endl;
    return 0;
}
""";
        List<TestCase> validParenthesesTests = List.of(
                new TestCase("()", "true", false),
                new TestCase("()[]{}", "true", false),
                new TestCase("(]", "false", false),
                new TestCase("([])", "true", true),
                new TestCase("([)]", "false", true),
                new TestCase("(((((((((())))))))))", "true", true)
        );
        Problem p2 = new Problem(
                "valid-parentheses",
                "Valid Parentheses",
                "Easy",
                "Given a string `s` containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.\n\nAn input string is valid if:\n1. Open brackets must be closed by the same type of brackets.\n2. Open brackets must be closed in the correct order.\n3. Every close bracket has a corresponding open bracket of the same type.",
                validParenthesesExamples,
                validParenthesesConstraints,
                validParenthesesStarterCode,
                validParenthesesStarterCodeCpp,
                validParenthesesTests
        );
        problems.put(p2.getId(), p2);

        // Problem 3: Palindrome Number
        List<String> palindromeExamples = List.of(
                "Input:\n121\nOutput:\ntrue\nExplanation: 121 reads as 121 from left to right and from right to left.",
                "Input:\n-121\nOutput:\nfalse\nExplanation: From left to right, it reads -121. From right to left, it becomes 121-. Therefore it is not a palindrome."
        );
        List<String> palindromeConstraints = List.of(
                "-2^31 <= x <= 2^31 - 1",
                "Could you solve it without converting the integer to a string?"
        );
        String palindromeStarterCode = """
import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextLong()) return;
        long x = sc.nextLong();
        System.out.println(isPalindrome(x));
    }

    public static boolean isPalindrome(long x) {
        if (x < 0) return false;
        long original = x;
        long reversed = 0;
        while (x > 0) {
            reversed = reversed * 10 + (x % 10);
            x /= 10;
        }
        return original == reversed;
    }
}
""";
        String palindromeStarterCodeCpp = """
#include <iostream>

using namespace std;

class Solution {
public:
    static bool isPalindrome(long long x) {
        if (x < 0) return false;
        long long original = x, reversed = 0;
        while (x > 0) {
            reversed = reversed * 10 + (x % 10);
            x /= 10;
        }
        return original == reversed;
    }
};

int main() {
    long long x;
    if (!(cin >> x)) return 0;
    cout << (Solution::isPalindrome(x) ? "true" : "false") << endl;
    return 0;
}
""";
        List<TestCase> palindromeTests = List.of(
                new TestCase("121", "true", false),
                new TestCase("-121", "false", false),
                new TestCase("10", "false", false),
                new TestCase("123454321", "true", true),
                new TestCase("0", "true", true)
        );
        Problem p3 = new Problem(
                "palindrome-number",
                "Palindrome Number",
                "Easy",
                "Given an integer `x`, return `true` if `x` is a palindrome, and `false` otherwise.\n\nAn integer is a palindrome when it reads the same forward and backward.",
                palindromeExamples,
                palindromeConstraints,
                palindromeStarterCode,
                palindromeStarterCodeCpp,
                palindromeTests
        );
        problems.put(p3.getId(), p3);

        // Problem 4: Maximum Subarray
        List<String> maxSubarrayExamples = List.of(
                "Input:\n9\n-2 1 -3 4 -1 2 1 -5 4\nOutput:\n6\nExplanation: The subarray [4, -1, 2, 1] has the largest sum 6.",
                "Input:\n1\n1\nOutput:\n1",
                "Input:\n5\n5 4 -1 7 8\nOutput:\n23"
        );
        List<String> maxSubarrayConstraints = List.of(
                "1 <= nums.length <= 10^5",
                "-10^4 <= nums[i] <= 10^4"
        );
        String maxSubarrayStarterCode = """
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
        System.out.println(maxSubArray(nums));
    }

    public static int maxSubArray(int[] nums) {
        int currentMax = nums[0];
        int globalMax = nums[0];
        for (int i = 1; i < nums.length; i++) {
            currentMax = Math.max(nums[i], currentMax + nums[i]);
            globalMax = Math.max(globalMax, currentMax);
        }
        return globalMax;
    }
}
""";
        String maxSubarrayStarterCodeCpp = """
#include <iostream>
#include <vector>
#include <algorithm>

using namespace std;

class Solution {
public:
    static int maxSubArray(vector<int>& nums) {
        int currentMax = nums[0];
        int globalMax = nums[0];
        for (size_t i = 1; i < nums.size(); i++) {
            currentMax = max(nums[i], currentMax + nums[i]);
            globalMax = max(globalMax, currentMax);
        }
        return globalMax;
    }
};

int main() {
    int n;
    if (!(cin >> n)) return 0;
    vector<int> nums(n);
    for (int i = 0; i < n; i++) cin >> nums[i];
    cout << Solution::maxSubArray(nums) << endl;
    return 0;
}
""";
        List<TestCase> maxSubarrayTests = List.of(
                new TestCase("9\n-2 1 -3 4 -1 2 1 -5 4", "6", false),
                new TestCase("1\n1", "1", false),
                new TestCase("5\n5 4 -1 7 8", "23", false),
                new TestCase("4\n-5 -2 -8 -1", "-1", true),
                new TestCase("6\n-2 -1 -3 -4 -1 -2", "-1", true)
        );
        Problem p4 = new Problem(
                "maximum-subarray",
                "Maximum Subarray",
                "Medium",
                "Given an integer array `nums`, find the subarray with the largest sum, and return its sum.\n\nA subarray is a contiguous non-empty sequence of elements within an array.",
                maxSubarrayExamples,
                maxSubarrayConstraints,
                maxSubarrayStarterCode,
                maxSubarrayStarterCodeCpp,
                maxSubarrayTests
        );
        problems.put(p4.getId(), p4);

        // Problem 5: Climbing Stairs
        List<String> climbStairsExamples = List.of(
                "Input:\n2\nOutput:\n2\nExplanation: There are two ways to climb to the top:\n1. 1 step + 1 step\n2. 2 steps",
                "Input:\n3\nOutput:\n3\nExplanation: There are three ways to climb to the top:\n1. 1 step + 1 step + 1 step\n2. 1 step + 2 steps\n3. 2 steps + 1 step"
        );
        List<String> climbStairsConstraints = List.of(
                "1 <= n <= 45"
        );
        String climbStairsStarterCode = """
import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        System.out.println(climbStairs(n));
    }

    public static int climbStairs(int n) {
        if (n <= 2) return n;
        int first = 1, second = 2;
        for (int i = 3; i <= n; i++) {
            int third = first + second;
            first = second;
            second = third;
        }
        return second;
    }
}
""";
        String climbStairsStarterCodeCpp = """
#include <iostream>

using namespace std;

class Solution {
public:
    static int climbStairs(int n) {
        if (n <= 2) return n;
        int first = 1, second = 2;
        for (int i = 3; i <= n; i++) {
            int third = first + second;
            first = second;
            second = third;
        }
        return second;
    }
};

int main() {
    int n;
    if (!(cin >> n)) return 0;
    cout << Solution::climbStairs(n) << endl;
    return 0;
}
""";
        List<TestCase> climbStairsTests = List.of(
                new TestCase("2", "2", false),
                new TestCase("3", "3", false),
                new TestCase("4", "5", false),
                new TestCase("5", "8", true),
                new TestCase("10", "89", true),
                new TestCase("35", "14930352", true)
        );
        Problem p5 = new Problem(
                "climbing-stairs",
                "Climbing Stairs",
                "Easy",
                "You are climbing a staircase. It takes `n` steps to reach the top.\n\nEach time you can either climb 1 or 2 steps. In how many distinct ways can you climb to the top?",
                climbStairsExamples,
                climbStairsConstraints,
                climbStairsStarterCode,
                climbStairsStarterCodeCpp,
                climbStairsTests
        );
        problems.put(p5.getId(), p5);
    }

    public List<Problem> findAll() {
        return new ArrayList<>(problems.values());
    }

    public Optional<Problem> findById(String id) {
        return Optional.ofNullable(problems.get(id));
    }
}
