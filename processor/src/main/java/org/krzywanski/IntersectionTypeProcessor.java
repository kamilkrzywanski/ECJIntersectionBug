package org.krzywanski;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("org.krzywanski.Annot")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class IntersectionTypeProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement anno = processingEnv.getElementUtils().getTypeElement("org.krzywanski.Annot");
        if (anno == null) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(anno)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[IntersectionTypeProcessor] Processing element: " + element);
            scanElement(element);
        }
        return false;
    }

    private void scanElement(Element element) {
        switch (element.getKind()) {
            case CLASS:
            case INTERFACE:
            case ENUM:
            case RECORD:
                TypeElement typeElement = (TypeElement) element;
                for (TypeParameterElement tpe : typeElement.getTypeParameters()) {
                    handleTypeParameterBounds(tpe);
                }
                break;
            case METHOD:
                ExecutableElement method = (ExecutableElement) element;
                for (TypeParameterElement tpe : method.getTypeParameters()) {
                    handleTypeParameterBounds(tpe);
                }
                scanType(method.getReturnType());
                for (VariableElement param : method.getParameters()) {
                    scanType(param.asType());
                }
                break;
            default:
                break;
        }
        for (Element enclosed : element.getEnclosedElements()) {
            scanElement(enclosed);
        }
    }

    private void handleTypeParameterBounds(TypeParameterElement tpe) {
        List<? extends TypeMirror> bounds = tpe.getBounds();
        if (bounds != null && bounds.size() > 1) {
            StringBuilder sb = new StringBuilder("Znaleziono intersection type (bounds): ");
            for (int i = 0; i < bounds.size(); i++) {
                if (i > 0) sb.append(" & ");
                sb.append(bounds.get(i).toString());
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, sb.toString());
        }
        if (bounds != null) {
            for (TypeMirror b : bounds) {
                scanType(b);
            }
        }
        scanType(tpe.asType());
    }

    private void scanType(TypeMirror typeMirror) {
        if (typeMirror == null) return;
        switch (typeMirror.getKind()) {
            case INTERSECTION:
                IntersectionType intersectionType = (IntersectionType) typeMirror;
                StringBuilder sb = new StringBuilder();
                sb.append("Found intersection type: ");
                for (int i = 0; i < intersectionType.getBounds().size(); i++) {
                    if (i > 0) sb.append(" & ");
                    sb.append(intersectionType.getBounds().get(i).toString());
                }
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, sb.toString());
                break;
            case TYPEVAR:
                TypeVariable tv = (TypeVariable) typeMirror;
                scanType(tv.getUpperBound());
                scanType(tv.getLowerBound());
                break;
            case WILDCARD:
                WildcardType wt = (WildcardType) typeMirror;
                scanType(wt.getExtendsBound());
                scanType(wt.getSuperBound());
                break;
            case DECLARED:
                for (TypeMirror arg : ((DeclaredType) typeMirror).getTypeArguments()) {
                    scanType(arg);
                }
                break;
            default:
                break;
        }
    }
}
