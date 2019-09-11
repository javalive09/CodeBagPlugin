package com.javalive09.codebag;

import com.android.ddmlib.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Plugin extends AnAction {

    private String methodName;
    private String filePathName;
    private static final String ACTION = "com.javalive09.ACTION_CODEBAG";
    private static final String ANNOTATION = "@Run";
    private static final String SPLIT = "src/main/java/";

    private static void log(String msg) {
        PluginManager.getLogger().info(msg);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            String projectPath = project.getBasePath();
            log("actionPerformed ====== projectPath");

            shellFile(projectPath, "gradlew", "installDebug");

            int index = filePathName.indexOf(SPLIT);
            String classNamePath = filePathName.substring(index + SPLIT.length(), filePathName.length() - 5);
            String className = classNamePath.replace("/", ".");
            String cmd = "am start -a " + ACTION + " --es className " + className
                    + " --es methodName " + methodName;

            try {
                exeDeviceCmd(cmd);
            } catch (Exception ex) {
                ex.printStackTrace();
                log("exeDeviceCmd exception:" + ex.getMessage());
            }
        }
    }

    private void exeDeviceCmd(String cmd) {
        AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
        if (bridge == null) {
            AndroidDebugBridge.init(true);
            bridge = AndroidDebugBridge.createBridge();
//            bridge = AndroidDebugBridge.createBridge("/Users/peter/Library/Android/sdk/platform-tools/adb", false);
            log("bridge:" + bridge);
            waitForDevice(bridge);
        }
        IDevice[] devices = bridge.getDevices();
        if (devices != null) {
            IDevice device = devices[0];
            log("device:" + device);
            exeDeviceCmd(device, cmd);
            log("exeDeviceCmd result = ");
        }
    }

    private void exeDeviceCmd(IDevice device, String cmd) {
        try {
            device.executeShellCommand(cmd, new MultiLineReceiver() {
                @Override
                public void processNewLines(String[] strings) {
                    for (String string : strings) {
                        log("processNewLines:" + string);
                    }
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            });
        } catch (TimeoutException | ShellCommandUnresponsiveException | AdbCommandRejectedException | IOException e) {
            e.printStackTrace();
            log("exeDeviceCmd=" + e.getMessage());
        }
    }

    private static void waitForDevice(AndroidDebugBridge bridge) {
        int count = 0;
        while (!bridge.hasInitialDeviceList()) {
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException ignored) {
            }
            if (count > 300) {
                log("Time out");
                break;
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        String method = getMethodName(e, ANNOTATION);
        if (method != null && method.length() > 0) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setIcon(AllIcons.RunConfigurations.TestState.Run);
            e.getPresentation().setText(method);
        } else {
            e.getPresentation().setVisible(false);
        }
        methodName = method;
    }

    private String getMethodName(AnActionEvent e, String annotationName) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        if (editor != null) {
            final SelectionModel selectionModel = editor.getSelectionModel();
            String selectText = selectionModel.getSelectedText();
            if (project != null) {
                PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
                if (psiFile != null) {
                    filePathName = psiFile.getVirtualFile().getPath();
                    log("filePathName=" + filePathName);
                    PsiElement[] psiElements = psiFile.getChildren();
                    for (PsiElement psiElement : psiElements) {
                        if (psiElement instanceof PsiClass) {
                            PsiClass psiClass = (PsiClass) psiElement;
                            for (PsiMethod method : psiClass.getMethods()) {
                                if (method.getName().equals(selectText)) {
                                    PsiModifierList psiModifierList = method.getModifierList();
                                    PsiAnnotation[] annotations = psiModifierList.getAnnotations();
                                    for (PsiAnnotation annotation : annotations) {
                                        if (annotation.getText().contains(annotationName)) {
                                            return selectText;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void shellFile(String path, String fileName, String... order) {
        ArrayList<String> arrayList = new ArrayList<String>() {{
            add("/bin/sh");
            add(fileName);
            addAll(Arrays.asList(order));
        }};
        ProcessBuilder process = new ProcessBuilder(arrayList);
        process.directory(new File(path));
        try {
            Process p = process.start();
            getProcessString(p);
        } catch (Exception e) {
            e.printStackTrace();
            log("exception ====== " + e.getMessage());
        }
    }

    private void getProcessString(Process process) throws Exception {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        int exitVal = process.waitFor();
        log("exitVal！======= " + exitVal);

        if (exitVal == 0) {
            log("Success！======= ");
            log("output ========" + output);
            log("Success!");
        } else {
            log("Failed！======= ");
        }
    }

}
