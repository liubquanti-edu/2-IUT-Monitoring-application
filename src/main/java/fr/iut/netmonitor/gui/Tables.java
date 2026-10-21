package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.model.MeasurementType;
import fr.iut.netmonitor.model.NetworkDevice;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Outils pour construire et mettre à jour les tableaux. */
public final class Tables {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Tables() {
    }

    public static <S, T> TableColumn<S, T> column(String title, Function<S, T> value, double width) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(value.apply(c.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    /** Colonne dont le texte reçoit une classe CSS « prefix-VALEUR » (couleur selon l'état ou la gravité). */
    public static <S, T extends Enum<T>> TableColumn<S, T> enumColumn(String title, Function<S, T> value,
                                                                       String cssPrefix, double width) {
        TableColumn<S, T> col = column(title, value, width);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(s -> s.startsWith(cssPrefix + "-"));
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                    getStyleClass().add(cssPrefix + "-" + item.name());
                }
            }
        });
        return col;
    }

    /** Remplace le contenu en conservant la sélection et le tri. */
    public static <S> void update(TableView<S> table, List<S> items, ToIntFunction<S> id) {
        S selected = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(items);
        table.sort();
        if (selected != null) {
            int selectedId = id.applyAsInt(selected);
            items.stream()
                    .filter(i -> id.applyAsInt(i) == selectedId)
                    .findFirst()
                    .ifPresent(i -> table.getSelectionModel().select(i));
        }
    }

    public static String dateTime(LocalDateTime t) {
        return t == null ? "-" : DATE_TIME.format(t);
    }

    public static String latest(NetworkDevice device, MeasurementType type) {
        return device.getLatestValue(type)
                .map(v -> String.format("%.0f %s", v, type.getUnit()))
                .orElse("-");
    }

    public static String orDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
