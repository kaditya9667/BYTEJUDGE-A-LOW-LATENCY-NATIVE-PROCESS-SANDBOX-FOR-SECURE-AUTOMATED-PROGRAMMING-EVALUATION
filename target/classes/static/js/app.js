/**
 * JavaJudge — LeetCode-Style Online Judge & Sandbox (Java 21 & C++ 20)
 * Unified Problem Solver, Monaco Editor, Multi-Language,
 * SSE Live Streaming, Submissions History, and Security Sandbox.
 */

document.addEventListener('DOMContentLoaded', () => {
    let editor = null;
    let isExecuting = false;
    let currentMode = 'judge'; // 'judge' or 'playground'
    let currentLanguage = 'java'; // 'java' or 'cpp'
    let currentProblemId = 'two-sum';
    let currentProblemData = null;
    let publicTestCases = [];
    let activeTestCaseIndex = 0; // 0, 1, 2... or -1 for Custom Input

    // --- Built-in Code Templates for Playground Mode ---
    const PLAYGROUND_TEMPLATES = {
        java: {
            hello: `// Java 21 Online Compiler & Sandbox
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, JavaJudge!");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Sandbox Memory Cap: -Xmx64M");
    }
}
`,
            sorting: `import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int[] numbers = { 64, 34, 25, 12, 22, 11, 90 };
        System.out.println("Original array: " + Arrays.toString(numbers));
        Arrays.sort(numbers);
        System.out.println("Sorted array:   " + Arrays.toString(numbers));
    }
}
`,
            fibonacci: `public class Main {
    public static void main(String[] args) {
        int n = 15;
        System.out.println("Fibonacci first " + n + " terms:");
        long a = 0, b = 1;
        for (int i = 1; i <= n; i++) {
            System.out.print(a + (i < n ? ", " : "\\n"));
            long next = a + b;
            a = b;
            b = next;
        }
    }
}
`,
            tle: `public class Main {
    public static void main(String[] args) {
        System.out.println("Infinite loop watchdog test (5s limit)...");
        while (true) {}
    }
}
`,
            security_test: `public class Main {
    public static void main(String[] args) {
        // Disallowed by sandbox security validator
        System.exit(0);
    }
}
`
        },
        cpp: {
            hello: `// C++ 20 Online Compiler (GCC 16)
#include <iostream>

int main() {
    std::cout << "Hello, JavaJudge C++20 Sandbox!" << std::endl;
    std::cout << "Compiler: MinGW-W64 GCC 16.1.0" << std::endl;
    return 0;
}
`,
            sorting: `#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> nums = {64, 34, 25, 12, 22, 11, 90};
    std::sort(nums.begin(), nums.end());
    std::cout << "Sorted array: ";
    for (int x : nums) std::cout << x << " ";
    std::cout << std::endl;
    return 0;
}
`,
            fibonacci: `#include <iostream>

int main() {
    int n = 15;
    std::cout << "Fibonacci first " << n << " terms:" << std::endl;
    long long a = 0, b = 1;
    for (int i = 1; i <= n; i++) {
        std::cout << a << (i < n ? ", " : "\\n");
        long long next = a + b;
        a = b;
        b = next;
    }
    return 0;
}
`,
            tle: `#include <iostream>

int main() {
    std::cout << "Entering C++ infinite loop (5s watchdog)..." << std::endl;
    while (true) {}
    return 0;
}
`,
            security_test: `#include <iostream>
#include <cstdlib>

int main() {
    // Disallowed by sandbox security validator
    system("dir");
    return 0;
}
`
        }
    };

    // --- DOM Elements ---
    // Header
    const problemSelect = document.getElementById('problem-select');
    const languageSelect = document.getElementById('language-select');
    const problemDifficultyBadge = document.getElementById('problem-difficulty-badge');
    const playgroundTemplateWrapper = document.getElementById('playground-template-wrapper');
    const templateSelect = document.getElementById('template-select');
    const btnModeJudge = document.getElementById('btn-mode-judge');
    const btnModePlayground = document.getElementById('btn-mode-playground');
    const btnRunCode = document.getElementById('btn-run-code');
    const btnSubmit = document.getElementById('btn-submit');
    const fileTabIcon = document.getElementById('file-tab-icon');

    // Problem Pane
    const problemPane = document.getElementById('problem-pane');
    const btnToggleProblemPane = document.getElementById('btn-toggle-problem-pane');
    const btnPrevProblem = document.getElementById('btn-prev-problem');
    const btnNextProblem = document.getElementById('btn-next-problem');
    const problemNumTitle = document.getElementById('problem-num-title');
    const problemPaneDifficulty = document.getElementById('problem-pane-difficulty');
    const problemTestCount = document.getElementById('problem-test-count');
    const problemDescription = document.getElementById('problem-description');
    const problemExamples = document.getElementById('problem-examples');
    const problemConstraints = document.getElementById('problem-constraints');

    // Editor Pane
    const editorPane = document.getElementById('editor-pane');
    const activeFilename = document.getElementById('active-filename');
    const btnCopyCode = document.getElementById('btn-copy-code');
    const btnDownloadCode = document.getElementById('btn-download-code');
    const btnFormat = document.getElementById('btn-format');
    const btnFullscreenEditor = document.getElementById('btn-fullscreen-editor');
    const btnClearCode = document.getElementById('btnClearCode') || document.getElementById('btn-clear-code');
    const btnResetCode = document.getElementById('btnResetCode') || document.getElementById('btn-reset-code');
    const editorLoader = document.getElementById('editor-loader');

    // Console Tabs & Status
    const tabNavTestcase = document.getElementById('tab-nav-testcase');
    const tabNavResult = document.getElementById('tab-nav-result');
    const tabNavSubmissions = document.getElementById('tab-nav-submissions');
    const tabNavTerminal = document.getElementById('tab-nav-terminal');

    const panelTestcase = document.getElementById('panel-testcase');
    const panelResult = document.getElementById('panel-result');
    const panelSubmissions = document.getElementById('panel-submissions');
    const panelTerminal = document.getElementById('panel-terminal');

    const statusPill = document.getElementById('status-pill');
    const statusPillText = document.getElementById('status-pill-text');
    const btnClearConsole = document.getElementById('btn-clear-console');

    // Testcase Panel
    const caseTabsBar = document.getElementById('case-tabs-bar');
    const testcaseInputEditor = document.getElementById('testcase-input-editor');
    const customInputHint = document.getElementById('custom-input-hint');

    // Test Result Panel
    const resultPlaceholder = document.getElementById('result-placeholder');
    const verdictBanner = document.getElementById('verdict-banner');
    const verdictBadge = document.getElementById('verdict-badge');
    const verdictRuntime = document.getElementById('verdict-runtime');
    const verdictPercentile = document.getElementById('verdict-percentile');
    const verdictTestsCount = document.getElementById('verdict-tests-count');
    const verdictSummaryMsg = document.getElementById('verdict-summary-msg');
    const compileErrorCard = document.getElementById('compile-error-card');
    const compileErrorIcon = document.getElementById('compile-error-icon');
    const compileErrorTitle = document.getElementById('compile-error-title');
    const compileErrorText = document.getElementById('compile-error-text');
    const resultCasePillsBar = document.getElementById('result-case-pills-bar');
    const resultCaseDetails = document.getElementById('result-case-details');
    const detailInput = document.getElementById('detail-input');
    const detailOutput = document.getElementById('detail-output');
    const detailExpected = document.getElementById('detail-expected');

    // Submissions Panel
    const submissionsList = document.getElementById('submissions-list');
    const submissionsCountBadge = document.getElementById('submissions-count-badge');

    // Terminal Panel
    const execBanner = document.getElementById('exec-banner');
    const bannerStatusTag = document.getElementById('banner-status-tag');
    const bannerTime = document.getElementById('banner-time');
    const bannerExit = document.getElementById('banner-exit');
    const bannerMsg = document.getElementById('banner-msg');
    const terminalWelcome = document.getElementById('terminal-welcome');
    const termStdout = document.getElementById('term-stdout');
    const termStderr = document.getElementById('term-stderr');

    // Resizers
    const paneResizerV = document.getElementById('pane-resizer-v');
    const paneResizerH = document.getElementById('pane-resizer-h');
    const editorPane = document.getElementById('editor-pane');
    const consolePane = document.getElementById('console-pane');

    // --- 1. Monaco Editor Initialization ---
    function initMonaco() {
        require.config({ paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.45.0/min/vs' } });
        require(['vs/editor/editor.main'], function () {
            editor = monaco.editor.create(document.getElementById('monaco-editor'), {
                value: '// Loading starter code...',
                language: 'java',
                theme: 'vs-dark',
                fontSize: 14,
                fontFamily: "'Fira Code', 'JetBrains Mono', Consolas, monospace",
                fontLigatures: true,
                automaticLayout: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                lineNumbers: 'on',
                renderLineHighlight: 'all',
                bracketPairColorization: { enabled: true },
                tabSize: 4,
                padding: { top: 10, bottom: 10 }
            });

            editorLoader.classList.add('fade-out');

            editor.onDidChangeModelContent(() => {
                saveUserCode();
                updateFilenameFromCode();
            });

            // Keyboard Shortcuts
            editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
                handleRunAction();
            });

            editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.Enter, () => {
                if (currentMode === 'judge') {
                    handleSubmitAction();
                } else {
                    handleRunAction();
                }
            });

            // Populate Playground templates
            updatePlaygroundTemplateOptions();

            // Initial problem load
            loadProblemList();
        });
    }

    function saveUserCode() {
        if (!editor) return;
        const key = currentMode === 'judge'
            ? `judge_code_${currentProblemId}_${currentLanguage}`
            : `judge_code___playground___${currentLanguage}`;
        localStorage.setItem(key, editor.getValue());
    }

    function getSavedUserCode(problemId, defaultCode) {
        const key = currentMode === 'judge'
            ? `judge_code_${problemId}_${currentLanguage}`
            : `judge_code___playground___${currentLanguage}`;
        return localStorage.getItem(key) || defaultCode;
    }

    function updateFilenameFromCode() {
        if (!editor) return;
        if (currentLanguage === 'cpp') {
            activeFilename.textContent = currentMode === 'judge' ? 'solution.cpp' : 'main.cpp';
            fileTabIcon.className = 'fa-solid fa-file-code file-icon';
            fileTabIcon.style.color = '#00599c';
        } else {
            const code = editor.getValue();
            const match = code.match(/public\s+class\s+([A-Za-z0-9_$]+)/);
            if (match && match[1]) {
                activeFilename.textContent = match[1] + '.java';
            } else {
                activeFilename.textContent = currentMode === 'judge' ? 'Solution.java' : 'Main.java';
            }
            fileTabIcon.className = 'fa-brands fa-java file-icon';
            fileTabIcon.style.color = '#e38c00';
        }
    }

    // --- 2. Language Switching (Java vs C++) ---
    languageSelect.addEventListener('change', (e) => {
        currentLanguage = e.target.value;
        if (editor) {
            monaco.editor.setModelLanguage(editor.getModel(), currentLanguage === 'cpp' ? 'cpp' : 'java');
        }
        updateFilenameFromCode();
        updatePlaygroundTemplateOptions();

        if (currentMode === 'judge' && currentProblemData) {
            const defaultCode = currentLanguage === 'cpp'
                ? (currentProblemData.starterCodeCpp || currentProblemData.starterCode)
                : currentProblemData.starterCode;
            const userCode = getSavedUserCode(currentProblemData.id, defaultCode);
            if (editor) editor.setValue(userCode);
        } else if (currentMode === 'playground') {
            const defaultCode = PLAYGROUND_TEMPLATES[currentLanguage].hello;
            const userCode = getSavedUserCode('__playground__', defaultCode);
            if (editor) editor.setValue(userCode);
        }

        showToast(`Switched language to ${currentLanguage === 'cpp' ? 'C++20 (GCC)' : 'Java 21 (JDK)'}`);
    });

    function updatePlaygroundTemplateOptions() {
        templateSelect.innerHTML = '';
        if (currentLanguage === 'cpp') {
            templateSelect.innerHTML = `
                <option value="hello">Hello World (C++)</option>
                <option value="sorting">Vector Sorting (STL)</option>
                <option value="fibonacci">Fibonacci Recursion</option>
                <option value="tle">Infinite Loop Guard (5s TLE)</option>
                <option value="security_test">Security Policy Check</option>
            `;
        } else {
            templateSelect.innerHTML = `
                <option value="hello">Hello World (Java)</option>
                <option value="sorting">Array Sorting & Search</option>
                <option value="fibonacci">Fibonacci Recursion</option>
                <option value="tle">Infinite Loop Guard (5s TLE)</option>
                <option value="security_test">Security Policy Check</option>
            `;
        }
    }

    // --- 3. Problem Loading & Rendering ---
    async function loadProblemList() {
        try {
            const resp = await fetch('/api/problems');
            if (resp.ok) {
                const problems = await resp.json();
                populateProblemSelect(problems);
            }
        } catch (e) {
            console.error('Error fetching problem list:', e);
        }
        loadProblem(currentProblemId);
    }

    function populateProblemSelect(problems) {
        problemSelect.innerHTML = '';
        problems.forEach((p, idx) => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = `${idx + 1}. ${p.title} (${p.difficulty})`;
            problemSelect.appendChild(opt);
        });

        const pgOpt = document.createElement('option');
        pgOpt.value = '__playground__';
        pgOpt.textContent = 'Playground (Scratchpad)';
        problemSelect.appendChild(pgOpt);

        problemSelect.value = currentProblemId;
    }

    async function loadProblem(problemId) {
        if (problemId === '__playground__') {
            switchMode('playground');
            return;
        }

        switchMode('judge');
        currentProblemId = problemId;
        problemSelect.value = problemId;

        try {
            const resp = await fetch(`/api/problems/${problemId}`);
            if (!resp.ok) throw new Error('Problem not found');
            const data = await resp.json();
            currentProblemData = data;
            renderProblemDetails(data);
        } catch (e) {
            console.error('Error fetching problem details:', e);
            showToast('Failed to load problem details');
        }
    }

    function renderProblemDetails(data) {
        problemNumTitle.textContent = data.title;
        setDifficultyBadge(problemDifficultyBadge, data.difficulty);
        setDifficultyBadge(problemPaneDifficulty, data.difficulty);
        problemTestCount.innerHTML = `<i class="fa-solid fa-vial"></i> ${data.testCases ? data.testCases.length : 0} Tests`;

        problemDescription.innerHTML = formatMarkdown(data.description || '');

        problemExamples.innerHTML = '';
        if (data.examples && data.examples.length > 0) {
            data.examples.forEach((exText, idx) => {
                const card = document.createElement('div');
                card.className = 'example-card';
                card.innerHTML = parseExampleText(exText, idx + 1);
                problemExamples.appendChild(card);
            });
        }

        problemConstraints.innerHTML = '';
        if (data.constraints && data.constraints.length > 0) {
            data.constraints.forEach(c => {
                const li = document.createElement('li');
                li.textContent = c;
                problemConstraints.appendChild(li);
            });
        }

        // Code
        const defaultCode = currentLanguage === 'cpp'
            ? (data.starterCodeCpp || data.starterCode)
            : data.starterCode;
        const userCode = getSavedUserCode(data.id, defaultCode);
        if (editor) {
            editor.setValue(userCode);
            updateFilenameFromCode();
        }

        publicTestCases = data.testCases || [];
        setupTestcaseTabs();
    }

    function setDifficultyBadge(el, diff) {
        if (!el) return;
        el.className = 'difficulty-pill';
        const lower = (diff || 'Easy').toLowerCase();
        el.classList.add(lower);
        el.textContent = diff;
    }

    function parseExampleText(rawText, num) {
        let input = '', output = '', explanation = '';
        const lines = rawText.split('\n');
        let currentSection = '';

        for (const line of lines) {
            if (line.startsWith('Input:')) {
                currentSection = 'input';
                input += line.replace('Input:', '').trim();
            } else if (line.startsWith('Output:')) {
                currentSection = 'output';
                output += line.replace('Output:', '').trim();
            } else if (line.startsWith('Explanation:')) {
                currentSection = 'explanation';
                explanation += line.replace('Explanation:', '').trim();
            } else {
                if (currentSection === 'input') input += '\n' + line;
                else if (currentSection === 'output') output += '\n' + line;
                else if (currentSection === 'explanation') explanation += '\n' + line;
            }
        }

        return `
            <div class="example-card-title">Example ${num}</div>
            <div class="example-row">
                <span class="example-label">Input</span>
                <pre class="example-code">${escapeHtml(input.trim())}</pre>
            </div>
            <div class="example-row">
                <span class="example-label">Output</span>
                <pre class="example-code">${escapeHtml(output.trim())}</pre>
            </div>
            ${explanation.trim() ? `
            <div class="example-row">
                <span class="example-label">Explanation</span>
                <div style="font-size:0.85rem; color:#cbd5e1;">${escapeHtml(explanation.trim())}</div>
            </div>` : ''}
        `;
    }

    function formatMarkdown(text) {
        return text
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/\n\n/g, '<br><br>');
    }

    function escapeHtml(str) {
        return (str || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    // --- 4. Testcase Tabs Management ---
    function setupTestcaseTabs() {
        caseTabsBar.innerHTML = '';
        activeTestCaseIndex = 0;

        publicTestCases.forEach((tc, idx) => {
            const btn = document.createElement('button');
            btn.className = `case-btn ${idx === 0 ? 'active' : ''}`;
            btn.textContent = `Case ${idx + 1}`;
            btn.addEventListener('click', () => selectTestCase(idx));
            caseTabsBar.appendChild(btn);
        });

        const customBtn = document.createElement('button');
        customBtn.className = 'case-btn';
        customBtn.innerHTML = '<i class="fa-solid fa-plus"></i> Custom';
        customBtn.title = 'Test arbitrary custom input';
        customBtn.addEventListener('click', () => selectTestCase(-1));
        caseTabsBar.appendChild(customBtn);

        selectTestCase(0);
    }

    function selectTestCase(idx) {
        activeTestCaseIndex = idx;
        const allBtns = caseTabsBar.querySelectorAll('.case-btn');
        allBtns.forEach((b, i) => {
            if (idx === -1) {
                b.classList.toggle('active', i === allBtns.length - 1);
            } else {
                b.classList.toggle('active', i === idx);
            }
        });

        if (idx === -1) {
            testcaseInputEditor.readOnly = false;
            testcaseInputEditor.value = localStorage.getItem('judge_custom_input') || '';
            testcaseInputEditor.placeholder = 'Type custom stdin input here...';
            customInputHint.classList.remove('hidden');
        } else {
            testcaseInputEditor.readOnly = true;
            testcaseInputEditor.value = publicTestCases[idx] ? publicTestCases[idx].input : '';
            customInputHint.classList.add('hidden');
        }
    }

    testcaseInputEditor.addEventListener('input', () => {
        if (activeTestCaseIndex === -1) {
            localStorage.setItem('judge_custom_input', testcaseInputEditor.value);
        }
    });

    // --- 5. Mode Switching (Problem vs Playground) ---
    function switchMode(mode) {
        currentMode = mode;
        if (mode === 'playground') {
            btnModeJudge.classList.remove('active');
            btnModePlayground.classList.add('active');
            problemPane.classList.add('collapsed');
            paneResizerV.classList.add('hidden');
            playgroundTemplateWrapper.classList.remove('hidden');
            problemDifficultyBadge.classList.add('hidden');
            btnSubmit.classList.add('hidden');
            btnRunCode.querySelector('span').textContent = 'Run';

            const defaultCode = PLAYGROUND_TEMPLATES[currentLanguage].hello;
            const pgCode = localStorage.getItem(`judge_code___playground___${currentLanguage}`) || defaultCode;
            if (editor) editor.setValue(pgCode);
            updateFilenameFromCode();

            switchConsoleTab('terminal');
            showToast('Switched to Playground');
        } else {
            btnModeJudge.classList.add('active');
            btnModePlayground.classList.remove('active');
            problemPane.classList.remove('collapsed');
            paneResizerV.classList.remove('hidden');
            playgroundTemplateWrapper.classList.add('hidden');
            problemDifficultyBadge.classList.remove('hidden');
            btnSubmit.classList.remove('hidden');
            btnRunCode.querySelector('span').textContent = 'Run';

            if (currentProblemData && editor) {
                const defaultCode = currentLanguage === 'cpp'
                    ? (currentProblemData.starterCodeCpp || currentProblemData.starterCode)
                    : currentProblemData.starterCode;
                const userCode = getSavedUserCode(currentProblemData.id, defaultCode);
                editor.setValue(userCode);
                updateFilenameFromCode();
            }

            switchConsoleTab('testcase');
        }
    }

    btnModeJudge.addEventListener('click', () => {
        if (currentMode !== 'judge') {
            if (problemSelect.value === '__playground__') {
                problemSelect.value = 'two-sum';
            }
            loadProblem(problemSelect.value);
        }
    });

    btnModePlayground.addEventListener('click', () => {
        if (currentMode !== 'playground') {
            problemSelect.value = '__playground__';
            switchMode('playground');
        }
    });

    problemSelect.addEventListener('change', (e) => {
        const val = e.target.value;
        if (val === '__playground__') {
            switchMode('playground');
        } else {
            loadProblem(val);
        }
    });

    templateSelect.addEventListener('change', (e) => {
        const key = e.target.value;
        const templates = PLAYGROUND_TEMPLATES[currentLanguage];
        if (templates && templates[key] && editor) {
            editor.setValue(templates[key]);
            saveUserCode();
            updateFilenameFromCode();
            showToast('Loaded: ' + templateSelect.options[templateSelect.selectedIndex].text);
        }
    });

    btnToggleProblemPane.addEventListener('click', () => {
        problemPane.classList.toggle('collapsed');
        if (problemPane.classList.contains('collapsed')) {
            paneResizerV.classList.add('hidden');
            btnToggleProblemPane.innerHTML = '<i class="fa-solid fa-angles-right"></i>';
        } else {
            paneResizerV.classList.remove('hidden');
            btnToggleProblemPane.innerHTML = '<i class="fa-solid fa-angles-left"></i>';
        }
    });

    if (btnPrevProblem) {
        btnPrevProblem.addEventListener('click', () => {
            const options = Array.from(problemSelect.options).filter(o => o.value !== '__playground__');
            const currentIndex = options.findIndex(o => o.value === currentProblemId);
            if (currentIndex > 0) {
                loadProblem(options[currentIndex - 1].value);
            }
        });
    }

    if (btnNextProblem) {
        btnNextProblem.addEventListener('click', () => {
            const options = Array.from(problemSelect.options).filter(o => o.value !== '__playground__');
            const currentIndex = options.findIndex(o => o.value === currentProblemId);
            if (currentIndex >= 0 && currentIndex < options.length - 1) {
                loadProblem(options[currentIndex + 1].value);
            }
        });
    }

    // --- 6. Console Tab Switching ---
    function switchConsoleTab(tabKey) {
        tabNavTestcase.classList.toggle('active', tabKey === 'testcase');
        tabNavResult.classList.toggle('active', tabKey === 'result');
        tabNavSubmissions.classList.toggle('active', tabKey === 'submissions');
        tabNavTerminal.classList.toggle('active', tabKey === 'terminal');

        panelTestcase.classList.toggle('active', tabKey === 'testcase');
        panelResult.classList.toggle('active', tabKey === 'result');
        panelSubmissions.classList.toggle('active', tabKey === 'submissions');
        panelTerminal.classList.toggle('active', tabKey === 'terminal');

        if (tabKey === 'submissions') {
            loadSubmissionsHistory();
        }
    }

    tabNavTestcase.addEventListener('click', () => switchConsoleTab('testcase'));
    tabNavResult.addEventListener('click', () => switchConsoleTab('result'));
    tabNavSubmissions.addEventListener('click', () => switchConsoleTab('submissions'));
    tabNavTerminal.addEventListener('click', () => switchConsoleTab('terminal'));

    // --- 7. Execution Engine (SSE Live Streaming & Run/Submit) ---
    function handleRunAction() {
        if (currentMode === 'judge') {
            runJudgeCode(false);
        } else {
            runPlaygroundCode();
        }
    }

    function handleSubmitAction() {
        runJudgeCode(true);
    }

    btnRunCode.addEventListener('click', handleRunAction);
    btnSubmit.addEventListener('click', handleSubmitAction);

    async function runJudgeCode(isSubmit) {
        if (isExecuting || !editor) return;

        const code = editor.getValue();
        if (!code.trim()) {
            showToast('Cannot execute: editor is empty');
            return;
        }

        isExecuting = true;
        const targetBtn = isSubmit ? btnSubmit : btnRunCode;
        const origContent = targetBtn.innerHTML;
        targetBtn.disabled = true;
        targetBtn.innerHTML = '<div class="spinner" style="width:13px;height:13px;border-width:2px;"></div> Evaluating...';
        setStatusPill('running', isSubmit ? 'Submitting...' : 'Running...');

        const evalBar = document.getElementById('eval-progress-bar');
        if (evalBar) evalBar.classList.add('evaluating');

        switchConsoleTab('result');
        resultPlaceholder.classList.add('hidden');
        verdictBanner.classList.add('hidden');
        compileErrorCard.classList.add('hidden');
        resultCasePillsBar.classList.add('hidden');
        resultCaseDetails.classList.add('hidden');
        verdictPercentile.classList.add('hidden');

        const customInput = (!isSubmit && activeTestCaseIndex === -1)
            ? (testcaseInputEditor.value || '')
            : null;

        // Render pending case pills
        const count = isSubmit ? (currentProblemData ? currentProblemData.testCases.length : 5) : (customInput != null ? 1 : publicTestCases.length);
        renderPendingPills(count);

        try {
            const resp = await fetch('/api/submissions/execute', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    problemId: currentProblemId,
                    language: currentLanguage,
                    code: code,
                    isSubmit: isSubmit,
                    customInput: customInput
                })
            });

            if (!resp.ok) throw new Error(`HTTP Error ${resp.status}`);
            const result = await resp.json();
            renderJudgeResult(result, isSubmit);
            mirrorLogsToTerminal(result);

        } catch (err) {
            console.error('Judge submission error:', err);
            setStatusPill('error', 'Execution Error');
            showToast('Execution failed: ' + err.message);
        } finally {
            isExecuting = false;
            targetBtn.disabled = false;
            targetBtn.innerHTML = origContent;
            if (evalBar) evalBar.classList.remove('evaluating');
        }
    }

    function renderPendingPills(total) {
        resultCasePillsBar.classList.remove('hidden');
        resultCasePillsBar.innerHTML = '';
        for (let i = 1; i <= total; i++) {
            const pill = document.createElement('button');
            pill.className = 'result-case-pill running';
            pill.innerHTML = `<span class="pill-dot running"></span> <span>Case ${i}</span>`;
            resultCasePillsBar.appendChild(pill);
        }
    }

    function renderJudgeResult(result, isSubmit) {
        verdictBanner.classList.remove('hidden');

        const verdict = result.overallVerdict || 'RUNTIME_ERROR';
        verdictBadge.textContent = formatVerdictText(verdict);
        verdictBadge.className = 'verdict-tag';

        if (verdict === 'ACCEPTED') {
            verdictBadge.classList.add('success');
            setStatusPill('success', isSubmit ? 'Accepted' : 'Sample Tests Passed');
            if (isSubmit && result.percentileBeaten > 0) {
                verdictPercentile.classList.remove('hidden');
                verdictPercentile.innerHTML = `<i class="fa-solid fa-chart-simple"></i> Beats ${result.percentileBeaten}%`;
            }
        } else if (verdict === 'TIME_LIMIT_EXCEEDED') {
            verdictBadge.classList.add('tle');
            setStatusPill('tle', 'Time Limit Exceeded');
        } else if (verdict === 'SECURITY_VIOLATION') {
            verdictBadge.classList.add('security');
            setStatusPill('error', 'Security Policy Violation');
        } else {
            verdictBadge.classList.add('error');
            setStatusPill('error', verdict === 'COMPILATION_ERROR' ? 'Compilation Error' : 'Wrong Answer');
        }

        verdictRuntime.innerHTML = `<i class="fa-regular fa-clock"></i> ${result.totalExecutionTimeMs || 0} ms`;
        verdictTestsCount.innerHTML = `<i class="fa-solid fa-check-double"></i> ${result.passedTests} / ${result.totalTests} Passed`;
        verdictSummaryMsg.textContent = result.message || '';

        // Check for Compilation or Security Error
        if (verdict === 'COMPILATION_ERROR' || verdict === 'SECURITY_VIOLATION') {
            compileErrorCard.classList.remove('hidden');
            if (verdict === 'SECURITY_VIOLATION') {
                compileErrorIcon.className = 'fa-solid fa-shield-halved';
                compileErrorTitle.textContent = 'Sandbox Security Policy Violation:';
            } else {
                compileErrorIcon.className = 'fa-solid fa-triangle-exclamation';
                compileErrorTitle.textContent = 'Compiler Diagnostics:';
            }
            compileErrorText.textContent = result.compilerError || 'Execution rejected.';
            return;
        }

        // Render test case pills & details
        const cases = result.testCaseResults || [];
        if (cases.length > 0) {
            resultCasePillsBar.classList.remove('hidden');
            resultCaseDetails.classList.remove('hidden');
            resultCasePillsBar.innerHTML = '';

            cases.forEach((c, idx) => {
                const pill = document.createElement('button');
                const isPass = c.status === 'ACCEPTED';
                pill.className = `result-case-pill ${isPass ? 'passed' : 'failed'} ${idx === 0 ? 'active' : ''}`;
                pill.innerHTML = `
                    <span class="pill-dot ${isPass ? 'pass' : 'fail'}"></span>
                    <span>Case ${c.testCaseIndex || idx + 1}</span>
                `;
                pill.addEventListener('click', () => {
                    resultCasePillsBar.querySelectorAll('.result-case-pill').forEach((p, i) => {
                        p.classList.toggle('active', i === idx);
                    });
                    displayCaseDetail(c);
                });
                resultCasePillsBar.appendChild(pill);
            });

            displayCaseDetail(cases[0]);
        }
    }

    function displayCaseDetail(caseResult) {
        detailInput.textContent = caseResult.input || '(None)';
        detailOutput.textContent = caseResult.actualOutput || (caseResult.status === 'TIME_LIMIT_EXCEEDED' ? 'Time Limit Exceeded (> 5.0s)' : '(No output)');
        detailExpected.textContent = caseResult.expectedOutput || (caseResult.isHidden ? '[Hidden Testcase]' : '(Arbitrary Custom Input)');
    }

    function formatVerdictText(v) {
        switch (v) {
            case 'ACCEPTED': return 'ACCEPTED';
            case 'WRONG_ANSWER': return 'WRONG ANSWER';
            case 'TIME_LIMIT_EXCEEDED': return 'TIME LIMIT EXCEEDED';
            case 'COMPILATION_ERROR': return 'COMPILATION ERROR';
            case 'SECURITY_VIOLATION': return 'SECURITY VIOLATION';
            case 'RUNTIME_ERROR': return 'RUNTIME ERROR';
            default: return v;
        }
    }

    function mirrorLogsToTerminal(result) {
        terminalWelcome.classList.add('hidden');
        termStdout.textContent = '';
        termStderr.classList.add('hidden');
        termStderr.textContent = '';

        if (result.compilerError) {
            termStderr.classList.remove('hidden');
            termStderr.textContent = result.compilerError;
        } else if (result.testCaseResults && result.testCaseResults.length > 0) {
            let outputAccum = '';
            result.testCaseResults.forEach(c => {
                outputAccum += `[Case ${c.testCaseIndex}] Status: ${c.status} (${c.executionTimeMs}ms)\n`;
                if (c.actualOutput) outputAccum += `Output: ${c.actualOutput}\n`;
                if (c.errorOutput) outputAccum += `Stderr: ${c.errorOutput}\n`;
                outputAccum += '\n';
            });
            termStdout.textContent = outputAccum.trim();
        }
    }

    // --- 8. Submissions History ---
    async function loadSubmissionsHistory() {
        submissionsList.innerHTML = '<div style="color:var(--text-dim); text-align:center; padding:16px;">Loading history...</div>';
        try {
            const resp = await fetch(`/api/submissions/history?problemId=${currentProblemId}`);
            if (resp.ok) {
                const history = await resp.json();
                renderSubmissionsList(history);
            }
        } catch (e) {
            console.error('Failed to load history:', e);
        }
    }

    function renderSubmissionsList(list) {
        submissionsCountBadge.textContent = `${list.length} Submissions`;
        if (list.length === 0) {
            submissionsList.innerHTML = '<div style="color:var(--text-dim); text-align:center; padding:20px;">No submissions yet. Click Submit to evaluate!</div>';
            return;
        }

        submissionsList.innerHTML = '';
        list.forEach(item => {
            const row = document.createElement('div');
            row.className = 'submission-item';
            const isPass = item.verdict === 'ACCEPTED';
            row.innerHTML = `
                <div class="submission-info">
                    <span class="verdict-tag ${isPass ? 'success' : 'error'}">${item.verdict}</span>
                    <span class="submission-lang-badge">${item.language}</span>
                    <span style="font-size:0.85rem; font-weight:600;"><i class="fa-regular fa-clock"></i> ${item.runtimeMs} ms</span>
                    <span class="submission-time">${item.submittedAt}</span>
                </div>
                <button class="btn-restore-code" title="Load this code into Monaco editor">Load Code</button>
            `;

            row.querySelector('.btn-restore-code').addEventListener('click', () => {
                if (editor && item.code) {
                    editor.setValue(item.code);
                    if (item.language && item.language !== currentLanguage) {
                        languageSelect.value = item.language;
                        languageSelect.dispatchEvent(new Event('change'));
                    }
                    saveUserCode();
                    updateFilenameFromCode();
                    showToast('Restored submission code');
                }
            });

            submissionsList.appendChild(row);
        });
    }

    // --- 9. Playground Freeform Execution ---
    async function runPlaygroundCode() {
        if (isExecuting || !editor) return;

        const code = editor.getValue();
        if (!code.trim()) {
            showToast('Cannot execute: editor is empty');
            return;
        }

        isExecuting = true;
        btnRunCode.disabled = true;
        btnRunCode.innerHTML = '<div class="spinner" style="width:13px;height:13px;border-width:2px;"></div> Running...';
        setStatusPill('running', 'Running...');

        const evalBar = document.getElementById('eval-progress-bar');
        if (evalBar) evalBar.classList.add('evaluating');

        switchConsoleTab('terminal');
        terminalWelcome.classList.add('hidden');
        termStdout.textContent = '';
        termStderr.classList.add('hidden');
        termStderr.textContent = '';
        execBanner.classList.add('hidden');

        const stdin = testcaseInputEditor.value || '';

        try {
            const resp = await fetch('/api/compiler/run', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    code: code,
                    stdin: stdin,
                    language: currentLanguage
                })
            });

            if (!resp.ok) throw new Error(`HTTP Error ${resp.status}`);
            const data = await resp.json();
            renderPlaygroundResult(data);

        } catch (err) {
            console.error('Playground execution error:', err);
            setStatusPill('error', 'Execution Error');
            termStderr.classList.remove('hidden');
            termStderr.textContent = `[System Error] ${err.message}`;
        } finally {
            isExecuting = false;
            btnRunCode.disabled = false;
            btnRunCode.innerHTML = '<i class="fa-solid fa-play"></i><span>Run</span><kbd class="shortcut-kbd">Ctrl+Enter</kbd>';
            if (evalBar) evalBar.classList.remove('evaluating');
        }
    }

    function renderPlaygroundResult(data) {
        execBanner.classList.remove('hidden');
        bannerTime.innerHTML = `<i class="fa-regular fa-clock"></i> ${data.executionTimeMs} ms`;
        bannerExit.innerHTML = `<i class="fa-solid fa-arrow-right-from-bracket"></i> Exit ${data.exitCode}`;
        bannerMsg.textContent = data.message || '';

        bannerStatusTag.className = 'banner-tag';
        if (data.status === 'SUCCESS') {
            bannerStatusTag.classList.add('success');
            bannerStatusTag.textContent = 'SUCCESS';
            setStatusPill('success', `Finished (${data.executionTimeMs}ms)`);
        } else if (data.status === 'TIME_LIMIT_EXCEEDED') {
            bannerStatusTag.classList.add('tle');
            bannerStatusTag.textContent = 'TIMEOUT (TLE)';
            setStatusPill('tle', 'Time Limit Exceeded');
        } else if (data.status === 'SECURITY_VIOLATION') {
            bannerStatusTag.className = 'banner-tag error';
            bannerStatusTag.textContent = 'SECURITY VIOLATION';
            setStatusPill('error', 'Security Policy Blocked');
        } else {
            bannerStatusTag.classList.add('error');
            bannerStatusTag.textContent = data.status === 'COMPILATION_ERROR' ? 'COMPILATION ERROR' : 'ERROR';
            setStatusPill('error', data.status === 'COMPILATION_ERROR' ? 'Compilation Error' : `Exit ${data.exitCode}`);
        }

        if (data.stdout && data.stdout.trim().length > 0) {
            termStdout.textContent = data.stdout;
        } else if (data.status === 'SUCCESS') {
            termStdout.textContent = '(Program produced no standard output)';
        }

        if (data.stderr && data.stderr.trim().length > 0) {
            termStderr.classList.remove('hidden');
            termStderr.textContent = data.stderr;
        }
    }

    // --- 10. Code Formatting & Editor Actions ---
    if (btnCopyCode) {
        btnCopyCode.addEventListener('click', async () => {
            if (!editor) return;
            const code = editor.getValue();
            try {
                await navigator.clipboard.writeText(code);
                showToast('Code copied to clipboard');
            } catch (err) {
                const ta = document.createElement('textarea');
                ta.value = code;
                document.body.appendChild(ta);
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
                showToast('Code copied to clipboard');
            }
        });
    }

    if (btnDownloadCode) {
        btnDownloadCode.addEventListener('click', () => {
            if (!editor) return;
            const code = editor.getValue();
            if (!code.trim()) {
                showToast('Cannot download: editor is empty');
                return;
            }
            const filename = (activeFilename && activeFilename.textContent.trim())
                ? activeFilename.textContent.trim()
                : (currentLanguage === 'cpp' ? 'Solution.cpp' : 'Solution.java');

            const blob = new Blob([code], { type: 'text/plain;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            showToast(`Downloaded ${filename}`);
        });
    }

    if (btnFullscreenEditor && editorPane) {
        btnFullscreenEditor.addEventListener('click', () => {
            editorPane.classList.toggle('fullscreen-mode');
            const isFull = editorPane.classList.contains('fullscreen-mode');
            btnFullscreenEditor.innerHTML = isFull
                ? '<i class="fa-solid fa-compress"></i>'
                : '<i class="fa-solid fa-expand"></i>';
            btnFullscreenEditor.title = isFull ? 'Exit Fullscreen (Esc)' : 'Toggle Fullscreen Editor (Esc to exit)';
            if (editor) {
                setTimeout(() => editor.layout(), 60);
            }
            showToast(isFull ? 'Entered fullscreen editor (Press Esc to exit)' : 'Exited fullscreen editor');
        });
    }

    btnFormat.addEventListener('click', () => {
        if (!editor) return;
        const currentCode = editor.getValue();
        const formatted = formatCode(currentCode);
        editor.setValue(formatted);
        saveUserCode();
        updateFilenameFromCode();
        showToast('Code formatted');
    });

    function formatCode(code) {
        if (!code) return '';
        const lines = code.split('\n');
        let indent = 0;
        const formatted = [];
        for (let rawLine of lines) {
            let line = rawLine.trim();
            if (!line) continue;

            let leadingCloses = (line.match(/^[\}\]\)]+/) || [''])[0].length;
            let currentIndent = Math.max(0, indent - leadingCloses);
            formatted.push('    '.repeat(currentIndent) + line);

            let stripped = line.replace(/"(?:[^"\\]|\\.)*"/g, '');
            let opens = (stripped.match(/[\{\[\(]/g) || []).length;
            let closes = (stripped.match(/[\}\]\)]/g) || []).length;
            indent = Math.max(0, indent + opens - closes);
        }
        return formatted.join('\n');
    }

    btnClearCode.addEventListener('click', () => {
        if (!editor) return;
        editor.setValue('');
        editor.focus();
        saveUserCode();
        updateFilenameFromCode();
        showToast('Editor cleared');
    });

    btnResetCode.addEventListener('click', () => {
        if (!editor) return;
        if (currentMode === 'judge' && currentProblemData) {
            const defaultCode = currentLanguage === 'cpp'
                ? (currentProblemData.starterCodeCpp || currentProblemData.starterCode)
                : currentProblemData.starterCode;
            editor.setValue(defaultCode);
            saveUserCode();
            updateFilenameFromCode();
            showToast('Reset to starter code');
        } else if (currentMode === 'playground') {
            const key = templateSelect.value;
            const templates = PLAYGROUND_TEMPLATES[currentLanguage];
            if (templates && templates[key]) {
                editor.setValue(templates[key]);
                saveUserCode();
                updateFilenameFromCode();
                showToast('Reset to template');
            }
        }
    });

    btnClearConsole.addEventListener('click', () => {
        termStdout.textContent = '';
        termStderr.classList.add('hidden');
        termStderr.textContent = '';
        execBanner.classList.add('hidden');
        verdictBanner.classList.add('hidden');
        compileErrorCard.classList.add('hidden');
        resultCasePillsBar.classList.add('hidden');
        resultCaseDetails.classList.add('hidden');
        resultPlaceholder.classList.remove('hidden');
        terminalWelcome.classList.remove('hidden');
        setStatusPill('ready', 'Ready');
        showToast('Console cleared');
    });

    function setStatusPill(state, text) {
        statusPill.className = `status-pill ${state}`;
        statusPillText.textContent = text;
    }

    // --- 12. Resizable Split Panes ---
    let isResizingV = false;
    paneResizerV.addEventListener('mousedown', (e) => {
        isResizingV = true;
        paneResizerV.classList.add('resizing');
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
    });

    let isResizingH = false;
    paneResizerH.addEventListener('mousedown', (e) => {
        isResizingH = true;
        paneResizerH.classList.add('resizing');
        document.body.style.cursor = 'row-resize';
        document.body.style.userSelect = 'none';
    });

    document.addEventListener('mousemove', (e) => {
        if (isResizingV) {
            const containerWidth = document.querySelector('.workbench').offsetWidth;
            const newWidthPct = (e.clientX / containerWidth) * 100;
            if (newWidthPct >= 20 && newWidthPct <= 65) {
                problemPane.style.width = `${newWidthPct}%`;
            }
        }
        if (isResizingH) {
            const containerHeight = document.querySelector('.coding-area').offsetHeight;
            const offsetTop = document.querySelector('.coding-area').getBoundingClientRect().top;
            const mouseOffsetY = e.clientY - offsetTop;
            const newHeight = containerHeight - mouseOffsetY;
            if (newHeight >= 100 && newHeight <= containerHeight - 120) {
                consolePane.style.height = `${newHeight}px`;
            }
        }
    });

    document.addEventListener('mouseup', () => {
        if (isResizingV) {
            isResizingV = false;
            paneResizerV.classList.remove('resizing');
            document.body.style.cursor = '';
            document.body.style.userSelect = '';
        }
        if (isResizingH) {
            isResizingH = false;
            paneResizerH.classList.remove('resizing');
            document.body.style.cursor = '';
            document.body.style.userSelect = '';
        }
    });

    // --- 13. Toast Utility ---
    let toastTimeout = null;
    function showToast(message, duration = 2200) {
        let toast = document.querySelector('.app-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.className = 'app-toast';
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.classList.add('show');
        if (toastTimeout) clearTimeout(toastTimeout);
        toastTimeout = setTimeout(() => {
            toast.classList.remove('show');
        }, duration);
    }

    // --- 14. Global Keyboard Shortcuts (Ctrl+Enter, Ctrl+Shift+Enter, Esc) ---
    window.addEventListener('keydown', (e) => {
        // Exit fullscreen on Escape
        if (e.key === 'Escape' && editorPane && editorPane.classList.contains('fullscreen-mode')) {
            editorPane.classList.remove('fullscreen-mode');
            if (btnFullscreenEditor) {
                btnFullscreenEditor.innerHTML = '<i class="fa-solid fa-expand"></i>';
                btnFullscreenEditor.title = 'Toggle Fullscreen Editor (Esc to exit)';
            }
            if (editor) {
                setTimeout(() => editor.layout(), 60);
            }
            showToast('Exited fullscreen editor');
            return;
        }

        // Global Ctrl+Enter / Cmd+Enter (Run) & Ctrl+Shift+Enter (Submit)
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            if (editor && editor.hasTextFocus()) {
                return; // Handled directly by Monaco addCommand
            }
            e.preventDefault();
            if (e.shiftKey) {
                if (currentMode === 'judge') {
                    handleSubmitAction();
                } else {
                    handleRunAction();
                }
            } else {
                handleRunAction();
            }
        }
    });

    initMonaco();
});
