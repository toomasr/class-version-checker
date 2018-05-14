package com.toomasr.classversionchecker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipUtil;

public class Main {
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.out.println("Usage: java -jar class-version-checker.jar [DIRECTORY|FILE]");
      System.out.println();
      System.out.println(
          "If DIRECTORY is provided then the tool will scan the directory to find class files or .jar and .war files. For each class file found the class version is printed. For each class file found inside JAR or WAR file the class file version is printed.");
      System.out.println();
      System.out.println(
          "If FILE is provided the file is scanned for packaged class files and class version information is printed out for earch class file found. If file is a class file then the class version info is printed for the that particular class.");
      System.out.println();
      System.exit(0);
    }

    File userFile = new File(args[0]);
    System.out.println("User provided argument is " + userFile.getCanonicalPath());

    if (userFile.isDirectory()) {
      try (Stream<Path> paths = Files.walk(Paths.get(args[0]))) {
        paths
            .filter(Files::isRegularFile)
            .forEach(s -> {
              if (s.toString().toLowerCase().endsWith("class"))
                printClassVersionForClassFile(s.toFile());
              else if (s.toString().toLowerCase().endsWith("war")
                  || s.toString().toLowerCase().endsWith("jar")
                  || s.toString().toLowerCase().endsWith("zip"))
                checkClassVersionForZipFile(s.toFile());
            });
      }
    }
    else {
      if (userFile.getName().endsWith("class")) {
        printClassVersionForClassFile(userFile);
      }
      else {
        checkClassVersionForZipFile(userFile);
      }
    }

  }

  private static void checkClassVersionForZipFile(File userFile) {
    System.out.println("Processing " + userFile.getAbsolutePath());
    ZipUtil.iterate(userFile, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        if (zipEntry.getName().endsWith(".class")) {
          System.out.println(zipEntry.getName() + "; version: " + getClassFileVersion(in));
        }
      }
    });
  }

  private static String getClassFileVersion(InputStream in) {
    try (DataInputStream din = new DataInputStream(in)) {
      int magic = din.readInt();
      if (magic != 0xcafebabe) {
        return "Not a class file header";
      }

      int minor = din.readUnsignedShort();
      int major = din.readUnsignedShort();
      return major + "." + minor;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void printClassVersionForClassFile(File file) {
    try {
      System.out.println(file.getAbsolutePath() + "; version: " + getClassFileVersion(new FileInputStream(file)));
    }
    catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
