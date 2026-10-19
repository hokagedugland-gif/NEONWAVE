package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Tiny transparent offline-bootstrap-friendly Gradle launcher.
 * Replace this with the official Gradle wrapper using `gradle wrapper` if preferred.
 */
public final class GradleWrapperMain {
    public static void main(String[] args) throws Exception {
        Path project=Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path settings=project.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props=new Properties();try(InputStream in=Files.newInputStream(settings)){props.load(in);}
        String distribution=props.getProperty("distributionUrl");
        if(distribution==null||!distribution.startsWith("https://services.gradle.org/distributions/gradle-")) throw new SecurityException("Unexpected Gradle distribution URL");
        String version=distribution.substring(distribution.lastIndexOf('/')+1).replace("-bin.zip","");
        Path home=Paths.get(System.getProperty("user.home"),".gradle","wrapper","dists","neonwave");
        Path bin=home.resolve(version).resolve("bin").resolve(isWindows()?"gradle.bat":"gradle");
        if(!Files.isRegularFile(bin)){
            Files.createDirectories(home);
            Path zip=home.resolve("gradle-distribution.zip");
            System.out.println("Downloading trusted Gradle distribution: "+distribution);
            URL url=new URL(distribution);URLConnection connection=url.openConnection();connection.setConnectTimeout(30000);connection.setReadTimeout(180000);
            try(InputStream in=connection.getInputStream()){Files.copy(in,zip,StandardCopyOption.REPLACE_EXISTING);}
            try(ZipInputStream in=new ZipInputStream(Files.newInputStream(zip))){ZipEntry entry;while((entry=in.getNextEntry())!=null){Path dest=home.resolve(entry.getName()).normalize();if(!dest.startsWith(home))throw new SecurityException("Unsafe ZIP entry");if(entry.isDirectory())Files.createDirectories(dest);else{Files.createDirectories(dest.getParent());Files.copy(in,dest,StandardCopyOption.REPLACE_EXISTING);}}}
            Files.deleteIfExists(zip);bin.toFile().setExecutable(true);
        }
        if(!Files.isRegularFile(bin))throw new IOException("Gradle binary missing: "+bin);
        List<String> command=new ArrayList<>();command.add(bin.toString());command.addAll(Arrays.asList(args));
        ProcessBuilder pb=new ProcessBuilder(command).directory(project.toFile()).inheritIO();
        int code=pb.start().waitFor();System.exit(code);
    }
    private static boolean isWindows(){return System.getProperty("os.name").toLowerCase(Locale.US).contains("win");}
}
