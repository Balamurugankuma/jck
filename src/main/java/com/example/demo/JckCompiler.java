package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.UUID;
import java.util.regex.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/compile")
@CrossOrigin
public class JckCompiler {

    @PostMapping
    public String compileAndRun(@RequestBody CodeRequest request) throws Exception {

        String code = request.code;
        String input = request.input == null ? "" : request.input;

        String oldClassName = extractClassName(code);
        if (oldClassName == null) return "No class found in code!";

        String newClassName = "Main" + UUID.randomUUID().toString().substring(0,6);
        code = code.replaceFirst("class\\s+" + oldClassName, "class " + newClassName);

        File javaFile = new File(newClassName + ".java");
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(code);
        }

        /// COMPILE
        Process compile = new ProcessBuilder("javac", javaFile.getName()).start();
        String compileError = readStream(compile.getErrorStream());

        if (!compileError.isEmpty()) {
            delete(javaFile);
            return compileError;
        }

        /// RUN PROGRAM
        Process process = new ProcessBuilder("java", newClassName).start();

        BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        writer.write(input);
        writer.flush();
        writer.close();

        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            delete(javaFile);
            delete(new File(newClassName + ".class"));
            return "Time Limit Exceeded";
        }

        String output = readStream(process.getInputStream());
        String error = readStream(process.getErrorStream());

        delete(javaFile);
        delete(new File(newClassName + ".class"));

        return error.isEmpty() ? output : error;
    }

    private String readStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null)
            sb.append(line).append("\n");
        return sb.toString();
    }

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }

    private void delete(File f) {
        if (f.exists()) f.delete();
    }
}
