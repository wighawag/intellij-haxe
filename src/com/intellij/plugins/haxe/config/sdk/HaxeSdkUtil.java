package com.intellij.plugins.haxe.config.sdk;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeSdkUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil");
  private static final Pattern VERSION_MATCHER = Pattern.compile("(\\d+(\\.\\d+)+)");
  private static final String COMPILER_EXECUTABLE_NAME = "haxe";

  @Nullable
  public static HaxeSdkData testHaxeSdk(String path) {
    final String exePath = getCompilerPathByFolderPath(path);
    if (!folderExists(path) || !fileExists(exePath)) {
      return null;
    }

    final GeneralCommandLine command = new GeneralCommandLine();
    command.setExePath(exePath);
    command.addParameter("-help");
    command.setWorkDirectory(path);

    try {
      final ProcessOutput output = new CapturingProcessHandler(
        command.createProcess(),
        Charset.defaultCharset(),
        command.getCommandLineString()).runProcess();

      if (output.getExitCode() != 0) {
        LOG.error("haXe compiler exited with invalid exit code: " + output.getExitCode());
        return null;
      }

      final String outputString = output.getStdout();

      final Matcher matcher = VERSION_MATCHER.matcher(outputString);
      if (matcher.find()) {
        return new HaxeSdkData(path, matcher.group(1));
      }

      return null;
    }
    catch (ExecutionException e) {
      LOG.info("Exception while executing the process:", e);
      return null;
    }
  }

  @Nullable
  private static String suggestNekoBinPath() {
    String result = System.getenv("NEKOPATH");
    if (result != null) {
      result += getExecutableName("neko");
    }
    if (result != null && new File(result).exists()) {
      return result;
    }
    return null;
  }

  public static String getCompilerPathByFolderPath(String folderPath) {
    final File compilerFile = new File(folderPath, getExecutableName(COMPILER_EXECUTABLE_NAME));
    return compilerFile.getPath();
  }

  private static String getExecutableName(String name) {
    if (SystemInfo.isWindows) {
      return name + ".exe";
    }
    return name;
  }

  private static boolean folderExists(@Nullable String path) {
    return path != null && checkFolderExists(new File(path));
  }

  private static boolean checkFolderExists(@NotNull File file) {
    return file.exists() && file.isDirectory();
  }

  private static boolean fileExists(@Nullable String filePath) {
    return filePath != null && new File(filePath).exists();
  }
}