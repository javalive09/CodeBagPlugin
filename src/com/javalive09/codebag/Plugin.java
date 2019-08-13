package com.javalive09.codebag;

import com.intellij.icons.AllIcons;
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

public class Plugin extends AnAction {

    private String methodName;
    private String filePathName;
    private static final String ACTION = "com.javalive09.ACTION_CODEBAG";
    private static final String ANNOTATION = "@Run";
    private static final String SPLIT = "src/main/java/";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if(project != null) {
            String projectPath = project.getBasePath();
            System.out.println("projectPath=" + projectPath);
            shellFile(projectPath);
            int index = filePathName.indexOf(SPLIT);
            String classNamePath = filePathName.substring(index + SPLIT.length(), filePathName.length() - 5);
            String className = classNamePath.replace("/", ".");
            System.out.println("className=" + className);
            String cmd = "adb shell am start -a " + ACTION + " --es className " + className
                    + " --es methodName " + methodName;
            System.out.println("cmd=" + cmd);
            exeCmd(cmd);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        String method = getMethodName(e, ANNOTATION);
        if(method != null && method.length() > 0) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setIcon(AllIcons.RunConfigurations.TestState.Run);
            e.getPresentation().setText(method);
        }else {
            e.getPresentation().setVisible(false);
        }
        methodName = method;
    }

    private String getMethodName(AnActionEvent e, String annotationName) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        if(editor != null) {
            final SelectionModel selectionModel = editor.getSelectionModel();
            String selectText = selectionModel.getSelectedText();
            if(project != null) {
                PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
                if (psiFile != null) {
                    filePathName = psiFile.getVirtualFile().getPath();
                    System.out.println("filePathName=" + filePathName);
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

    private void shellFile(String path) {
        ProcessBuilder process = new ProcessBuilder("/bin/sh", "gradlew", "installDebug");
        process.directory(new File(path));
        try {
            Process p = process.start();
            getProcessString(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getProcessString(Process process) throws Exception{
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        int exitVal = process.waitFor();
        if (exitVal == 0) {
            System.out.println("Success!");
            System.out.println(output);
        } else {
            System.out.println("Failed!");
        }

    }

    private String exeCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println("Success!");
                System.out.println(output);
                return output.toString();
            } else {
                System.out.println("Failed!");
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
