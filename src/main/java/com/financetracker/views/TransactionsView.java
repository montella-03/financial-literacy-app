package com.financetracker.views;

import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import com.financetracker.service.TransactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Route(value = "transactions", layout = MainLayout.class)
@PageTitle("Transactions · Steady")
public class TransactionsView extends VerticalLayout {

    private final TransactionService service;
    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);

    public TransactionsView(TransactionService service, com.financetracker.service.TransactionImportService imports) {
        this.service = service;
        setPadding(false);
        setSpacing(false);
        setSizeFull();
        addClassName("app-content");

        add(PageSupport.hero(
                "Cash movement",
                "Every shilling,<br/><em>accounted for.</em>",
                "Income and expenses in one calm list. Categorize once — advice gets sharper."
        ));

        Button add = PageSupport.primary("Add transaction");
        add.addClickListener(e -> openEditor(new Transaction()));

        HorizontalLayout toolbar = new HorizontalLayout(add, new Button("Import CSV", e -> openImport(imports)), FeatureSupport.download("CSV template", "transactions-template.csv", () -> "date,type,amount,category,merchant,description,account,destination\r\n"));
        toolbar.setPadding(false);
        toolbar.setWidthFull();
        toolbar.addClassName("view-toolbar");

        configureGrid();
        Div section = PageSupport.section("Ledger", "Newest first. Edit or remove anytime.", toolbar, grid);
        add(section);
        refresh();
    }

    private static void notifyError(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void openImport(com.financetracker.service.TransactionImportService imports) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import transactions");
        dialog.setWidth("700px");
        com.vaadin.flow.component.textfield.TextArea csv = new com.vaadin.flow.component.textfield.TextArea("Paste CSV including the header row");
        csv.setWidthFull();
        csv.setHeight("280px");
        csv.setMaxLength(2_000_000);
        String importOwner = com.financetracker.session.AppSession.ownerId();
        com.vaadin.flow.component.upload.Upload upload = new com.vaadin.flow.component.upload.Upload((com.vaadin.flow.server.streams.UploadHandler) event -> {
            byte[] bytes = event.getInputStream().readNBytes(2_000_001);
            if (bytes.length > 2_000_000) {
                event.reject("CSV exceeds 2 MB.");
                return;
            }
            event.getUI().accessSynchronously(() -> {
                if (!importOwner.equals(com.financetracker.session.AppSession.ownerId()))
                    throw new SecurityException("Please sign in again");
                csv.setValue(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            });
        });
        upload.setMaxFiles(1);
        upload.setMaxFileSize(2_000_000);
        upload.setAcceptedFileExtensions(".csv");
        upload.setUploadButton(new Button("Choose CSV file"));
        dialog.add(upload);
        dialog.add(new com.vaadin.flow.component.html.Paragraph("Use the template headers. Types: INCOME, EXPENSE, TRANSFER. Dates: YYYY-MM-DD. Exact matching rows are skipped; review legitimate identical payments manually. Imports are atomic: invalid rows save nothing."), csv);
        Button run = PageSupport.primary("Import");
        run.addClickListener(e -> FeatureSupport.attempt(() -> {
            var result = imports.importCsv(csv.getValue());
            refresh();
            dialog.close();
            Notification.show(result.imported() + " imported; " + result.duplicates() + " duplicates skipped.");
        }));
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), run);
        dialog.open();
    }

    private void configureGrid() {
        grid.addClassName("steady-grid");
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addColumn(tx -> tx.getTransactionDate() == null
                        ? "—"
                        : tx.getTransactionDate().toLocalDate().toString())
                .setHeader("Date")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(tx -> tx.getType() == null ? "—" : tx.getType().name())
                .setHeader("Type")
                .setAutoWidth(true);
        grid.addColumn(Transaction::getCategory).setHeader("Category").setFlexGrow(1);
        grid.addColumn(Transaction::getAccountName).setHeader("Account");
        grid.addColumn(Transaction::getMerchant).setHeader("Merchant").setFlexGrow(1);
        grid.addColumn(tx -> MoneyFormat.kes(tx.getAmount())).setHeader("Amount").setAutoWidth(true);
        grid.addComponentColumn(tx -> {
            Button edit = new Button("Edit", e -> openEditor(tx));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            Button delete = new Button("Delete", e -> confirmDelete(tx));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
    }

    private void refresh() {
        grid.setItems(service.findAll());
    }

    private void openEditor(Transaction transaction) {
        boolean create = transaction.getId() == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(create ? "Add transaction" : "Edit transaction");
        dialog.setWidth("480px");

        ComboBox<TransactionType> type = new ComboBox<>("Type");
        type.setItems(TransactionType.values());
        type.setValue(transaction.getType() != null ? transaction.getType() : TransactionType.EXPENSE);
        type.setWidthFull();

        BigDecimalField amount = new BigDecimalField("Amount");
        amount.setValue(transaction.getAmount());
        amount.setWidthFull();

        TextField category = new TextField("Category");
        category.setValue(nullToEmpty(transaction.getCategory()));
        category.setWidthFull();

        TextField merchant = new TextField("Merchant");
        merchant.setValue(nullToEmpty(transaction.getMerchant()));
        merchant.setWidthFull();

        TextField description = new TextField("Description");
        description.setValue(nullToEmpty(transaction.getDescription()));
        description.setWidthFull();

        DatePicker date = new DatePicker("Date");
        date.setValue(transaction.getTransactionDate() != null
                ? transaction.getTransactionDate().toLocalDate()
                : LocalDate.now());
        date.setWidthFull();

        TextField account = new TextField("Account / transfer source");
        account.setValue(nullToEmpty(transaction.getAccountName()));
        TextField destination = new TextField("Transfer destination");
        destination.setValue(nullToEmpty(transaction.getDestinationAccount()));
        FormLayout form = new FormLayout(type, amount, category, merchant, description, date, account, destination);
        dialog.add(form);

        Button cancel = new Button("Cancel", e -> dialog.close());
        Button save = PageSupport.primary("Save");
        save.addClickListener(e -> {
            if (type.getValue() == null || amount.getValue() == null || date.getValue() == null) {
                notifyError("Type, amount, and date are required.");
                return;
            }
            if (amount.getValue().signum() <= 0) {
                notifyError("Amount must be greater than zero.");
                return;
            }
            transaction.setType(type.getValue());
            transaction.setAmount(amount.getValue());
            transaction.setCategory(category.getValue());
            transaction.setMerchant(merchant.getValue());
            transaction.setDescription(description.getValue());
            transaction.setTransactionDate(LocalDateTime.of(date.getValue(), LocalTime.of(12, 0)));
            transaction.setAccountName(account.getValue());
            transaction.setDestinationAccount(destination.getValue());
            try {
                service.save(transaction);
            } catch (IllegalArgumentException ex) {
                notifyError(ex.getMessage());
                return;
            }
            dialog.close();
            refresh();
            Notification.show("Saved", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(Transaction transaction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete transaction?");
        dialog.add("This cannot be undone.");
        Button cancel = new Button("Cancel", e -> dialog.close());
        Button delete = new Button("Delete", e -> {
            service.delete(transaction.getId());
            dialog.close();
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, delete);
        dialog.open();
    }
}
