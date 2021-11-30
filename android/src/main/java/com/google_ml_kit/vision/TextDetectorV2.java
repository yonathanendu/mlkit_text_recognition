package com.google_ml_kit.vision;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
//import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
//import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
//import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
//import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google_ml_kit.ApiDetectorInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class TextDetectorV2 implements ApiDetectorInterface{
    private static final String START = "vision#startTextDetectorV2";
    private static final String CLOSE = "vision#closeTextDetectorV2";

    private final Context context;
    private TextRecognizer textRecognizer ;
    private int scriptLang = -1;
    public TextDetectorV2(Context context) {
        this.context = context;
    }

    @Override
    public List<String> getMethodsKeys() {
        return new ArrayList<>(
                Arrays.asList(START,
                        CLOSE));
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        String method = call.method;
        if (method.equals(START)) {
            handleDetection(call, result);
        } else if (method.equals(CLOSE)) {
            closeDetector();
            result.success(null);
        } else {
            result.notImplemented();
        }
    }

    private void handleDetection(MethodCall call, final MethodChannel.Result result) {
        Map<String, Object> imageData = (Map<String, Object>) call.argument("imageData");
        InputImage inputImage = InputImageConverter.getInputImageFromData(imageData, context, result);
        if (inputImage == null) return;
        int lang = (int) call.argument("language");
        if(scriptLang!=lang || textRecognizer==null) intitializeDetector(lang);

        textRecognizer.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        Map<String, Object> textResult = new HashMap<>();

                        textResult.put("text", text.getText());

                        List<Map<String, Object>> textBlocks = new ArrayList<>();
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            Map<String, Object> blockData = new HashMap<>();

                            addData(blockData,
                                    block.getText(),
                                    block.getBoundingBox(),
                                    block.getCornerPoints(),
                                    block.getRecognizedLanguage());

                            List<Map<String, Object>> textLines = new ArrayList<>();
                            for (Text.Line line : block.getLines()) {
                                Map<String, Object> lineData = new HashMap<>();

                                addData(lineData,
                                        line.getText(),
                                        line.getBoundingBox(),
                                        line.getCornerPoints(),
                                        line.getRecognizedLanguage());

                                List<Map<String, Object>> elementsData = new ArrayList<>();
                                for (Text.Element element : line.getElements()) {
                                    Map<String, Object> elementData = new HashMap<>();

                                    addData(elementData,
                                            element.getText(),
                                            element.getBoundingBox(),
                                            element.getCornerPoints(),
                                            element.getRecognizedLanguage());

                                    elementsData.add(elementData);
                                }
                                lineData.put("elements", elementsData);
                                textLines.add(lineData);
                            }
                            blockData.put("lines", textLines);
                            textBlocks.add(blockData);
                        }
                        textResult.put("blocks", textBlocks);
                        result.success(textResult);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        result.error("TextDetectorError", e.toString(), null);
                    }
                });
    }

    private void addData(Map<String, Object> addTo,
                         String text,
                         Rect rect,
                         Point[] cornerPoints,
                         String recognizedLanguage) {
        List<String> recognizedLanguages = new ArrayList<>();
        recognizedLanguages.add(recognizedLanguage);
        List<Map<String, Integer>> points = new ArrayList<>();
        addPoints(cornerPoints, points);
        addTo.put("points", points);
        addTo.put("rect", getBoundingPoints(rect));
        addTo.put("recognizedLanguages", recognizedLanguages);
        addTo.put("text", text);
    }

    private void addPoints(Point[] cornerPoints, List<Map<String, Integer>> points) {
        for (Point point : cornerPoints) {
            Map<String, Integer> p = new HashMap<>();
            p.put("x", point.x);
            p.put("y", point.y);
            points.add(p);
        }
    }

    private Map<String, Integer> getBoundingPoints(Rect rect) {
        Map<String, Integer> frame = new HashMap<>();
        frame.put("left", rect.left);
        frame.put("right", rect.right);
        frame.put("top", rect.top);
        frame.put("bottom", rect.bottom);
        return frame;
    }

    private void intitializeDetector(int script){
        closeDetector();
        Log.e("script lang", String.valueOf(script));
        scriptLang = script;
        switch (script){
            case 0 : textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                     break;
//            case 1: textRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
//                    break;
//            case 2: textRecognizer = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
//                    break;
//            case 3: textRecognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
//                    break;
//            case 4: textRecognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        }
    }
    private void closeDetector() {
        if(textRecognizer==null) return;
        textRecognizer.close();
        textRecognizer = null;
    }
}
