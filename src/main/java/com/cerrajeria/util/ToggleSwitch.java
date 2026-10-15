package com.cerrajeria.util;

import javafx.animation.TranslateTransition;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class ToggleSwitch extends StackPane {

    private static final double W = 48;
    private static final double H = 24;
    private static final double THUMB_R = 9;
    private static final double PADDING = 3;

    private final Region track = new Region();
    private final Circle thumb = new Circle(THUMB_R);
    private boolean selected = false;
    private Runnable onToggle;

    public ToggleSwitch() {
        setPrefSize(W, H);
        setMinSize(W, H);
        setMaxSize(W, H);
        setStyle("-fx-cursor: hand;");

        track.setPrefSize(W, H);
        track.setMinSize(W, H);
        track.setMaxSize(W, H);
        track.getStyleClass().addAll("toggle-track", "off");

        thumb.getStyleClass().addAll("toggle-thumb", "off");
        thumb.setTranslateX(-(W / 2 - THUMB_R - PADDING));

        getChildren().addAll(track, thumb);
        setOnMouseClicked(e -> toggle());
    }

    /** Callback que se ejecuta solo cuando el usuario hace clic. */
    public void setOnToggle(Runnable r) { this.onToggle = r; }

    public boolean isSelected() { return selected; }

    /** Cambia el estado SIN disparar onToggle (para inicializar). */
    public void setSelectedSilent(boolean value) {
        if (this.selected == value) return;
        this.selected = value;
        animate();
    }

    private void toggle() {
        selected = !selected;
        animate();
        if (onToggle != null) onToggle.run();
    }

    private void animate() {
        double destino = selected
                ? (W / 2 - THUMB_R - PADDING)
                : -(W / 2 - THUMB_R - PADDING);

        TranslateTransition tt = new TranslateTransition(Duration.millis(180), thumb);
        tt.setToX(destino);
        tt.play();

        track.getStyleClass().removeAll("on", "off");
        track.getStyleClass().add(selected ? "on" : "off");

        thumb.getStyleClass().removeAll("on", "off");
        thumb.getStyleClass().add(selected ? "on" : "off");
    }
}
