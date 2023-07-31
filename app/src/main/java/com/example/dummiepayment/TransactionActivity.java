package com.example.dummiepayment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.dummiepayment.constants.Transaction;
import com.example.dummiepayment.constants.TransactionResult;
import com.example.dummiepayment.utils.APIUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class TransactionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);
        continueTransaction();
        cancelTransaction();

        String md = getIntent().getStringExtra("DocumentData");

        EditText showXML = findViewById(R.id.editTextText);
        showXML.setText(md);

//        Intent intent = getIntent();
//        String md = intent.getStringExtra("SellerData");
    }

    public void continueTransaction(){

        Button continueButton = findViewById(R.id.continueTransaction);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent(getIntent().getAction());
                resultIntent.putExtra("TransactionResult", TransactionResult.ACCEPTED);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
    public void cancelTransaction(){

        Button cancelButton = findViewById(R.id.cancelTransaction);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent(getIntent().getAction());
                resultIntent.putExtra("ErrorMessage", "Transacción cancelada");
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        });
    }

    public void processSale() {

        // Obtenim el tipus de transacció
        String transactionType = getIntent().getStringExtra("TransactionType");
        switch (transactionType == null ? "" : transactionType) {
            case Transaction.SALE: {
                // Obtenim els paràmetres d'entrada
                String tenderType = getIntent().getStringExtra("TenderType");
                String currencyISO = getIntent().getStringExtra("CurrencyISO");
                BigDecimal amount = APIUtils.parseAPIAmount(getIntent().getStringExtra("Amount"));
                BigDecimal tipAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TipAmount"));
                BigDecimal taxAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TaxAmount"));
                int transactionId = getIntent().getIntExtra("TransactionId", -1);
                String transactionData = getIntent().getStringExtra("TransactionData");
                int receiptPrinterColumns = getIntent().getIntExtra("ReceiptPrinterColumns", 42);
                onSaleTransactionReceived(
                        tenderType, currencyISO, amount, tipAmount, taxAmount, transactionId,
                        transactionData, receiptPrinterColumns);
                break;
            }
            case Transaction.NEGATIVE_SALE: {
                String tenderType = getIntent().getStringExtra("TenderType");
                BigDecimal amount = APIUtils.parseAPIAmount(getIntent().getStringExtra("Amount"));
                int transactionId = getIntent().getIntExtra("TransactionId", -1);
                int receiptPrinterColumns = getIntent().getIntExtra("ReceiptPrinterColumns", -1);

                onNegativeSaleReceived(tenderType, amount, transactionId, receiptPrinterColumns);
                break;
            }
            case Transaction.REFUND: {
                String tenderType = getIntent().getStringExtra("TenderType");
                BigDecimal amount = APIUtils.parseAPIAmount(getIntent().getStringExtra("Amount"));
                int transactionId = getIntent().getIntExtra("TransactionId", -1);
                String transactionData = getIntent().getStringExtra("TransactionData");
                int receiptPrinterColumns = getIntent().getIntExtra("ReceiptPrinterColumns", 42);

                onRefundReceived(tenderType, amount, transactionId, transactionData, receiptPrinterColumns);
                break;
            }
            case Transaction.ADJUST_TIPS: {
                String tenderType = getIntent().getStringExtra("TenderType");
                BigDecimal amount = APIUtils.parseAPIAmount(getIntent().getStringExtra("Amount"));
                BigDecimal tipAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TipAmount"));
                int transactionId = getIntent().getIntExtra("TransactionId", -1);
                String transactionData = getIntent().getStringExtra("TransactionData");
                int receiptPrinterColumns = getIntent().getIntExtra("ReceiptPrinterColumns", 42);

                onAdjustTipsReceived(tenderType, amount, tipAmount, transactionId, transactionData, receiptPrinterColumns);
                break;
            }
            case Transaction.VOID_TRANSACTION: {
                String tenderType = getIntent().getStringExtra("TenderType");
                BigDecimal amount = APIUtils.parseAPIAmount(getIntent().getStringExtra("Amount"));
                BigDecimal taxAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TaxAmount"));
                BigDecimal tipAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TipAmount"));
                int transactionId = getIntent().getIntExtra("TransactionId", -1);
                int receiptPrinterColumns = getIntent().getIntExtra("ReceiptPrinterColumns", 42);

                onVoidTransactionReceived(tenderType, amount, taxAmount, tipAmount, transactionId, receiptPrinterColumns);
                break;
            }
            case Transaction.QUERY_TRANSACTION: {
                String tenderType = getIntent().getStringExtra("TenderType");
                BigDecimal amount = APIUtils.parseAPIAmount(getIntent().getStringExtra("Amount"));
                BigDecimal taxAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TaxAmount"));
                BigDecimal tipAmount = APIUtils.parseAPIAmount(getIntent().getStringExtra("TipAmount"));
                int transactionId = getIntent().getIntExtra("TransactionId", -1);
                int receiptPrinterColumns = getIntent().getIntExtra("ReceiptPrinterColumns", 42);

                onQueryTransactionReceived(tenderType, amount, taxAmount, tipAmount, transactionId, receiptPrinterColumns);
                break;
            }
            case Transaction.BATCH_CLOSE:
                onBatchCloseReceived();
                break;

            default:
                onUnknownTransactionReceived();
                break;
        }
    }

    public void onExit(View view) {
        processSale();
    }


    /* *********************************************************************** *
     *                   TRANSACTION MANAGEMENT EVENTS                         *
     * *********************************************************************** */


    /**
     * Event triggered when a sale transaction intent has thrown.
     */
    protected void onSaleTransactionReceived(
            String tenderType, String currencyISO,
            BigDecimal amount, BigDecimal tipAmount, BigDecimal taxAmount,
            int transactionId, String transactionData, int receiptPrinterColumns) {
        BigDecimal resultAmount = amount;

		/*String customerReceipt = buildCustomerReceipt(transactionId);
		String merchantReceipt = buildMerchantReceipt(transactionId);

		setTransactionResult(
				TransactionResult.ACCEPTED, Transaction.SALE,
				resultAmount, tipAmount, taxAmount,
				"1", merchantReceipt, customerReceipt, "");

		 */

        setTransactionResult(
                TransactionResult.ACCEPTED, Transaction.SALE,
                resultAmount, tipAmount, taxAmount,
                "1", "", "", "");
    }

    /**
     * Event triggered when a negative sale intent has thrown
     *
     * @param tenderType
     * @param amount
     * @param transactionId
     * @param receiptPrinterColumns
     */
    protected void onNegativeSaleReceived(
            String tenderType, BigDecimal amount, int transactionId, int receiptPrinterColumns) {
        setTransactionResult(
                TransactionResult.ACCEPTED, Transaction.NEGATIVE_SALE,
                amount, BigDecimal.ZERO, BigDecimal.ZERO,
                "", "", "", "");
    }


    /**
     * Event triggered when a refund intent has thrown
     *
     * @param tenderType
     * @param amount
     * @param transactionId
     * @param transactionData
     * @param receiptPrinterColumns
     */
    protected void onRefundReceived(
            String tenderType, BigDecimal amount, int transactionId, String transactionData,
            int receiptPrinterColumns) {
        String merchantReceipt = buildMerchantReceipt(transactionId);

        setTransactionResult(
                TransactionResult.ACCEPTED, Transaction.REFUND,
                amount, BigDecimal.ZERO, BigDecimal.ZERO,
                "", merchantReceipt, "", "");
    }


    /**
     * Event triggered when adjust tips intent has thrown
     *
     * @param tenderType
     * @param amount
     * @param tipAmount
     * @param transactionId
     * @param transactionData
     * @param receiptPrinterColumns
     */
    protected void onAdjustTipsReceived(
            String tenderType,
            BigDecimal amount, BigDecimal tipAmount, int transactionId,
            String transactionData, int receiptPrinterColumns) {
        tipAmount = BigDecimal.ONE;

        setTransactionResult(
                TransactionResult.ACCEPTED, Transaction.ADJUST_TIPS,
                amount, tipAmount, BigDecimal.ZERO,
                "", buildMerchantReceiptForAdjustTips(), "", "");
    }


    /**
     * Event triggered when transaction void intent has thrown
     *
     * @param tenderType
     * @param amount
     * @param taxAmount
     * @param tipAmount
     * @param transactionId
     * @param receiptPrinterColumns
     */
    protected void onVoidTransactionReceived(
            String tenderType, BigDecimal amount, BigDecimal taxAmount, BigDecimal tipAmount,
            int transactionId, int receiptPrinterColumns) {
        setTransactionResult(
                TransactionResult.UNKNOWN_RESULT, Transaction.VOID_TRANSACTION,
                amount, BigDecimal.ZERO, BigDecimal.ZERO,
                "", "", "", "");
    }

    /**
     * Event triggered when transaction query intent has thrown
     *
     * @param tenderType
     * @param amount
     * @param taxAmount
     * @param tipAmount
     * @param transactionId
     * @param receiptPrinterColumns
     */
    protected void onQueryTransactionReceived(
            String tenderType, BigDecimal amount, BigDecimal taxAmount, BigDecimal tipAmount,
            int transactionId, int receiptPrinterColumns) {
        String merchantReceipt = buildMerchantReceipt(transactionId);

        setTransactionResult(
                TransactionResult.ACCEPTED, Transaction.QUERY_TRANSACTION,
                amount, BigDecimal.ZERO, BigDecimal.ZERO,
                "", merchantReceipt, merchantReceipt, "");
    }


    /**
     * Event triggered when a batch close intent has thrown
     */
    protected void onBatchCloseReceived() {
        setBatchCloseResult(
                TransactionResult.ACCEPTED, Transaction.BATCH_CLOSE,
                new BigDecimal(1835.99), 1, "");
    }

    /**
     * Event triggered when an unknown transaction intent has thrown
     */
    private void onUnknownTransactionReceived() {
        Intent resultIntent = new Intent(getIntent().getAction());
        resultIntent.putExtra("ErrorMessage", "Method not supported");
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }


    /* *********************************************************************** *
     *                         ENDING TRANSACTION METHODS                      *
     * *********************************************************************** */

    /**
     * Set result of current transaction.
     *
     * @param transactionResult
     * @param transactionType
     * @param amount
     * @param tipAmount
     * @param taxAmount
     * @param transactionData
     * @param merchantReceipt
     * @param customerReceipt
     * @param errorMessage
     */
    protected void setTransactionResult(
            String transactionResult, String transactionType,
            BigDecimal amount, BigDecimal tipAmount, BigDecimal taxAmount,
            String transactionData, String merchantReceipt, String customerReceipt,
            String errorMessage) {
        Intent resultIntent = new Intent(getIntent().getAction());

        resultIntent.putExtra("TransactionResult", transactionResult);
        resultIntent.putExtra("TransactionType", transactionType);
        resultIntent.putExtra("Amount", APIUtils.serializeAPIAmount(amount));

        if (tipAmount.compareTo(BigDecimal.ZERO) != 0)
            resultIntent.putExtra("TipAmount", APIUtils.serializeAPIAmount(tipAmount));

        if (taxAmount.compareTo(BigDecimal.ZERO) != 0)
            resultIntent.putExtra("TaxAmount", APIUtils.serializeAPIAmount(taxAmount));

        if (transactionData != null && !transactionData.isEmpty())
            resultIntent.putExtra("TransactionData", transactionData);

        if (merchantReceipt != null && !merchantReceipt.isEmpty())
            resultIntent.putExtra("MerchantReceipt", merchantReceipt);

        if (customerReceipt != null && !customerReceipt.isEmpty())
            resultIntent.putExtra("CustomerReceipt", customerReceipt);

        if (errorMessage != null && !errorMessage.isEmpty())
            resultIntent.putExtra("ErrorMessage", errorMessage);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Set result of batch close.
     *
     * @param transactionResult
     * @param transactionType
     * @param amount
     * @param batchNumber
     * @param errorMessage
     */
    protected void setBatchCloseResult(
            String transactionResult, String transactionType, BigDecimal amount,
            int batchNumber, String errorMessage) {
        Intent resultIntent = new Intent(getIntent().getAction());

        resultIntent.putExtra("TransactionResult", transactionResult);
        resultIntent.putExtra("TransactionType", transactionType);

        if (amount.compareTo(BigDecimal.ZERO) != 0)
            resultIntent.putExtra("Amount", amount);

        resultIntent.putExtra("BatchNumber", String.valueOf(batchNumber));

        if (errorMessage != null && !errorMessage.isEmpty())
            resultIntent.putExtra("ErrorMessage", errorMessage);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Set result as an Exception executing current action.
     *
     * @param errorMessage
     */
    protected void setTransactionResultWithException(String errorMessage) {
        Intent resultIntent = new Intent(getIntent().getAction());
        resultIntent.putExtra("ErrorMessage", errorMessage);
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }


    private String buildCustomerReceipt(int transactionId) {
        String customerReceipt = "";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element receiptNode = document.createElement("Receipt");
            receiptNode.setAttribute("numCols", "42");
            document.appendChild(receiptNode);

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "CÓPIA CLIENTE¦§²³©®µãÃõÕ                       ");

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    " !\";#$%&'()*+,-./                             ");

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "0123456789:;=>                                 ");

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "TransactionId=" + transactionId);

            addReceiptLineToXMLDocument(document, receiptNode,
                    "CUT_PAPER", "NORMAL",
                    "");


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);

            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);

            // Escrivim el document en un String per enviar-lo a HioPosCloud
            transformer.transform(source, streamResult);
            customerReceipt = stringWriter.toString();
        } catch (TransformerException te) {
            System.out.println("TRANSFORMER EXCEPTION > " + te.getMessage());
            te.printStackTrace();
        } catch (ParserConfigurationException pce) {
            System.out.println("PARSER CONFIGURATION EXCEPTION > " + pce.getMessage());
            pce.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return customerReceipt;
    }

    private String buildMerchantReceipt(int transactionId) {
        String merchantReceipt = "";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element receiptNode = document.createElement("Receipt");
            receiptNode.setAttribute("numCols", "42");
            document.appendChild(receiptNode);

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "CÓPIA COMERCIO¦§²³©®µãÃõÕ                       ");

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    " !\";#$%&'()*+,-./                             ");

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "0123456789:;=>                                 ");

            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "TransactionId=" + transactionId);

            addReceiptLineToXMLDocument(document, receiptNode,
                    "CUT_PAPER", "NORMAL",
                    "");


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);

            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);

            // Escrivim el document en un String per enviar-lo a HioPosCloud
            transformer.transform(source, streamResult);
            merchantReceipt = stringWriter.toString();
        } catch (TransformerException te) {
            System.out.println("TRANSFORMER EXCEPTION > " + te.getMessage());
            te.printStackTrace();
        } catch (ParserConfigurationException pce) {
            System.out.println("PARSER CONFIGURATION EXCEPTION > " + pce.getMessage());
            pce.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return merchantReceipt;
    }

    public String buildMerchantReceiptForAdjustTips() {
        String merchantReceipt = "";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element receiptNode = document.createElement("Receipt");
            receiptNode.setAttribute("numCols", "42");
            document.appendChild(receiptNode);


            addReceiptLineToXMLDocument(document, receiptNode,
                    "TEXT", "NORMAL",
                    "ADJUST TIPS TRANSACTION                        ");

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);

            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);

            // Escrivim el document en un String per enviar-lo a HioPosCloud
            transformer.transform(source, streamResult);
            merchantReceipt = stringWriter.toString();
        } catch (TransformerException te) {
            te.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return merchantReceipt;
    }

    private void addReceiptLineToXMLDocument(Document document, Element rootElement,
                                             String type, String format, String value) {
        // ReceiptLine
        Element receiptLine = document.createElement("ReceiptLine");
        receiptLine.setAttribute("type", type);
        rootElement.appendChild(receiptLine);

        // Formats
        Element formatsNode = document.createElement("Formats");
        receiptLine.appendChild(formatsNode);

        Element formatNode = document.createElement("Format");
        formatNode.setAttribute("from", "0");
        formatNode.setAttribute("to", "42");
        formatNode.setTextContent(format);
        formatsNode.appendChild(formatNode);

        // Text
        Element textNode = document.createElement("Text");
        textNode.setTextContent(value);
        receiptLine.appendChild(textNode);
    }

}
