package com.example.demo;


import org.springframework.web.bind.annotation.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.util.UUID;
import java.util.regex.*;

@RestController
@RequestMapping("/jck/compiler")
public class JckCompiler {

    @PostMapping
    public String compileAndRun(@RequestBody String code) throws IOException {
        // Extract original class name
        String oldClassName = extractClassName(code);
        if (oldClassName == null) {
            return "No class found in code!";
        }

        // Generate random class name to avoid conflicts
        String newClassName = "Main" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String javaFileName = newClassName + ".java";
        String classFileName = newClassName + ".class";

        // Replace class name in code
        code = code.replaceFirst("class\\s+" + oldClassName, "class " + newClassName);

        // Write modified code to .java file
        try (FileWriter writer = new FileWriter(javaFileName)) {
            writer.write(code);
        }

        // Compile the .java file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, javaFileName);
        if (result != 0) {
            deleteFile(javaFileName); // Clean up on failure
            return "Compilation failed";
        }

        // Run the compiled class
        ProcessBuilder pb = new ProcessBuilder("java", newClassName);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read output from execution
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Clean up generated files
        deleteFile(javaFileName);
        deleteFile(classFileName);

        return output.toString();
    }

    // ✅ Helper: Extract the first class name from Java code
    private String extractClassName(String code) {
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // ✅ Helper: Delete file if it exists
    private void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}

