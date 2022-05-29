import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestsManager {
    /** LOGIN INFORMATION */
    private static String sender_id;
    private static String sender_password;
    private static String company_id;
    private static String user_id;
    private static String user_password;
    private static String temp_session_id;


    /** CONSTANTS */
    private static final String endpoint_url = "https://api.intacct.com/ia/xml/xmlgw.phtml";
    private static final String closer = "      </function>\n    </content>\n  </operation>\n</request>\n";
    private static Map<String, Integer> months;

    public static void init() {
        months = new HashMap<>();
        months.put("Jan", 1);
        months.put("Feb", 2);
        months.put("Mar", 3);
        months.put("Apr", 4);
        months.put("May", 5);
        months.put("Jun", 6);
        months.put("Jul", 7);
        months.put("Aug", 8);
        months.put("Sep", 9);
        months.put("Oct", 10);
        months.put("Nov", 11);
        months.put("Dec", 12);
    }


    /** HELPER METHODS */
    private static String postRequest(String request, boolean login) throws IOException {
        URL url = new URL(endpoint_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set timeout
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setUseCaches(true);
        connection.setRequestMethod("POST");

        // Set Headers
        connection.setRequestProperty("Accept", "application/xml");
        connection.setRequestProperty("Content-Type", "application/xml");

        // Write XML
        OutputStream outputStream = connection.getOutputStream();
        byte[] b = request.getBytes(StandardCharsets.UTF_8);
        outputStream.write(b);
        outputStream.flush();
        outputStream.close();

        try {
            int responseCode = connection.getResponseCode();
            String responseSuccessMessage = responseCode + " " + connection.getResponseMessage();
            if (responseCode != 200) {
                if (!login) {
                    Upload.appendTextArea("Some error occurred: " + responseSuccessMessage + "\n");
                }
                throw new IOException(responseSuccessMessage);
            }
        } catch(SocketTimeoutException e) {
            System.out.println("Get response code timed out");
        }

        // Read XML
        try {
            InputStream inputStream = connection.getInputStream();
            byte[] res = new byte[2048];
            int i;
            StringBuilder response = new StringBuilder();
            while ((i = inputStream.read(res)) != -1) {
                response.append(new String(res, 0, i));
            }
            inputStream.close();
            return response.toString();
        } catch (SocketTimeoutException e) {
            System.out.println("Get response code timed out");
        }

        return null;
    }

    private static String buildSageRequest(String action) {
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<request>\n" +
                "  <control>\n" +
                "    <senderid>" + sender_id + "</senderid>\n" +
                "    <password>" + sender_password + "</password>\n" +
                "    <controlid>{{$timestamp}}</controlid>\n" +
                "    <uniqueid>false</uniqueid>\n" +
                "    <dtdversion>3.0</dtdversion>\n" +
                "    <includewhitespace>false</includewhitespace>\n" +
                "  </control>\n" +
                "  <operation>\n" +
                "    <authentication>\n" +
                "      <sessionid>" + temp_session_id + "</sessionid>\n" +
                "    </authentication>\n" +
                "    <content>\n" +
                "      <function controlid=\"{{$guid}}\">\n";
        return header + action + closer;
    }

    private static String getErrorMessage(String response) throws ParserConfigurationException, IOException, SAXException {
        if (response == null) return "Unknown, but likely successful.\n";
        StringBuilder output = new StringBuilder();
        try {
            NodeList list = getListFromXML(response, "error");
            NodeList errorMessages = list.item(0).getChildNodes();
            output.append("Error \u2717\n    Error number: ").append(errorMessages.item(0).getTextContent());
            for(int i = 1; i < errorMessages.getLength(); i++) {
                output.append("\n    ").append(errorMessages.item(i).getTextContent());
            }
            output.append("\n");
        } catch (NullPointerException e) {
            return("Successful \u2713\n");
        }

        return output.toString();
    }

    private static NodeList getListFromXML(String response, String tag) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(response));
        Document doc = builder.parse(is);
        return doc.getElementsByTagName(tag);
    }

    private static void appendDate(StringBuilder sb, String s, String tag) {
        if (s == null) return;

        String date = "          <" + tag + ">\n" + "            <year>" + s.substring(s.length() - 4) + "</year>\n" +
                "            <month>" + months.get(s.substring(3, 6)) + "</month>\n" +
                "            <day>" + s.substring(0, 2) + "</day>\n" +
                "          </" + tag + ">\n";
        sb.append(date);
    }

    private static void appendTag(StringBuilder sb, String content, String tag) {
        if (content == null) return;

        sb.append("          <").append(tag).append(">").append(content).append("</").append(tag).append(">\n");
    }

    private static void appendItems(StringBuilder sb, List<String[]> lineItems) {
        // Line item headers: "LINE", "ITEMID", "QUANTITY", "UNIT", "LOCATIONID", "PRICE", "TAXABLE", "BILLING_TEMPLATE", "DEPARTMENTID",
        //                    "MEMO", "RETAINAGEPERCENTAGE", "PROJECTID", "CONVERSIONTYPE", "TASKID", "COSTTYPEID"
        for (String[] lineItem : lineItems) {
            sb.append("            <sotransitem>\n");
            appendItemTag(sb, lineItem[1], "itemid");
            appendItemTag(sb, lineItem[6], "taxable");
            appendItemTag(sb, lineItem[2], "quantity");
            appendItemTag(sb, lineItem[3], "unit");
            appendItemTag(sb, lineItem[5], "price");
            appendItemTag(sb, lineItem[4].substring(0, lineItem[4].length() - 2), "locationid");
            appendItemTag(sb, lineItem[8], "departmentid");
            appendItemTag(sb, lineItem[9], "memo");
            appendItemTag(sb, lineItem[11], "projectid");
            appendItemTag(sb, lineItem[13], "taskid");
            appendItemTag(sb, lineItem[14], "costtypeid");
            appendItemTag(sb, lineItem[7], "billingtemplate");
            appendItemTag(sb, lineItem[10], "retainagepercentage");
            appendItemTag(sb, lineItem[12], "conversiontype");
            sb.append("            </sotransitem>\n");
        }
    }

    private static void appendItemTag(StringBuilder sb, String content, String tag) {
        if (content == null) return;

        sb.append("              <").append(tag).append(">").append(content).append("</").append(tag).append(">\n");
    }

    public static String getRecordNoFromTDName(String tdName) throws ParserConfigurationException, IOException, SAXException {
        String function = "        <query>\n" +
                "          <object>PODOCUMENTPARAMS</object>\n" +
                "          <select>\n" +
                "            <field>DOCID</field>\n" +
                "            <field>RECORDNO</field>\n" +
                "            <field>DOCCLASS</field>\n" +
                "          </select>\n" +
                "        </query>";
        String request = buildSageRequest(function);
        String response = postRequest(request, false);
        String message = getErrorMessage(response);

        if (message.contains("Successful")) {
            NodeList docIDList = getListFromXML(response, "DOCID");
            NodeList recordNoList = getListFromXML(response, "RECORDNO");

            for (int i = 0; i < docIDList.getLength(); i++) {
                if (docIDList.item(i).getTextContent().trim().equalsIgnoreCase(tdName)) {
                    return recordNoList.item(i).getTextContent().trim();
                }
            }
        }

        return "Error \u2717: TD Name not found";
    }


    /** LOGIN METHODS */
    public static void setLoginCredentials(String senderID, String senderPassword, String companyID, String userID, String userPassword) {
        sender_id = senderID;
        sender_password = senderPassword;
        company_id = companyID;
        user_id = userID;
        user_password = userPassword;
    }

    public static void generateAPISession() throws IOException, ParserConfigurationException, SAXException {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<request>\n  <control>\n    <senderid>" + sender_id + "</senderid>\n    " +
                "<password>" + sender_password + "</password>\n    <controlid>{{$timestamp}}</controlid>\n    <uniqueid>false</uniqueid>\n    " +
                "<dtdversion>3.0</dtdversion>\n    <includewhitespace>false</includewhitespace>\n  </control>\n  <operation>\n    " +
                "<authentication>\n      <login>\n        <userid>" + user_id + "</userid>\n        <companyid>" + company_id + "</companyid>\n        " +
                "<password>" + user_password + "</password>\n      </login>\n    </authentication>\n    <content>\n      <function controlid=\"{{$guid}}\">\n" +
                "        <getAPISession />\n      </function>\n    </content>\n  </operation>\n</request>";
        String response = postRequest(request, true);
        temp_session_id = getListFromXML(response, "sessionid").item(0).getTextContent();
    }


    /** PROJECT ESTIMATE METHODS */
    public static List<String> queryField(String object, String field) throws IOException, ParserConfigurationException, SAXException {
        String function = "        <query>\n" +
                "          <object>" + object + "</object>\n" +
                "          <pagesize>1000</pagesize>" +
                "          <select>\n" +
                "            <field>" + field + "</field>\n" +
                "          </select>\n" +
                "        </query>";
        String request = buildSageRequest(function);
        String response = postRequest(request, false);
        String message = getErrorMessage(response);

        if (message.contains("Successful")) {
            NodeList estimateTypes = getListFromXML(response, field);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < estimateTypes.getLength(); i++) {
                list.add(estimateTypes.item(i).getTextContent().trim());
            }
            return list;
        }

        return null;
    }

    // Returns null if not found, and the tag if found
    public static String isFieldFound(String object, String key, String tag) throws ParserConfigurationException, IOException, SAXException {
        if (tag == null) tag = "RECORDNO";

        String function = "        <readByName>\n" +
                "          <object>" + object + "</object>\n" +
                "          <keys>" + key + "</keys>\n" +
                "          <fields>"+ tag + "</fields>\n" +
                "        </readByName>";
        String request = buildSageRequest(function);
        String response = postRequest(request, false);
        String message = getErrorMessage(response);

        if (message.contains("Successful")) {
            NodeList estimateTypes = getListFromXML(response, tag);
            if (estimateTypes.getLength() == 0) return null;
            return estimateTypes.item(0).getTextContent();
        }

        return null;
    }


    /** UPDATE / CREATE METHODS */
    public static void updatePurchasingTDs(List<String[]> allValues, int[] colNumbers, String recordNo) throws IOException, ParserConfigurationException, SAXException {
        StringBuilder sb = new StringBuilder();
        sb.append("        <update>\n");
        sb.append("            <PODOCUMENTPARAMS>\n");
        appendTag(sb, recordNo, "RECORDNO");
        appendTag(sb, "true", "POSTTOGL");

        sb.append("                <AR_ACCOUNTS>\n");
        for (String[] values : allValues) {
            addPurchasingTDHelper(
                    "AR_ACCOUNT",
                    sb,
                    values[colNumbers[1]],
                    values[colNumbers[3]],
                    values[colNumbers[2]],
                    values[colNumbers[4]]
            );
        }
        sb.append("                </AR_ACCOUNTS>\n");

        sb.append("                <INVGL_ACCOUNTS>\n");
        for (String[] values : allValues) {
            addPurchasingTDHelper(
                    "INVGL_ACCOUNT",
                    sb,
                    values[colNumbers[1]],
                    values[colNumbers[3]],
                    values[colNumbers[2]],
                    values[colNumbers[4]]
            );
        }
        sb.append("                </INVGL_ACCOUNTS>\n");

        sb.append("            </PODOCUMENTPARAMS>\n");
        sb.append("        </update>\n");

        String request = buildSageRequest(sb.toString());
        String response = postRequest(request, false);
        String message = getErrorMessage(response);
        Upload.appendTextArea(message);
    }

    private static void addPurchasingTDHelper(String tag, StringBuilder sb, String itemGlGroup, String glAccount, String debitCredit, String isOffset) {
        if (glAccount.endsWith(".0")) {
            glAccount = glAccount.substring(0, glAccount.length() - 2);
        }

        itemGlGroup = itemGlGroup.replaceAll("&", "&amp;");

        sb.append("                    <").append(tag).append(">\n");
        if (!itemGlGroup.isEmpty()) appendTag(sb, itemGlGroup, "ITEM_GLGROUP");
        appendTag(sb, glAccount, "GLACCOUNT");
        appendTag(sb, debitCredit, "DEBIT_CREDIT");
        appendTag(sb, isOffset.toLowerCase(), "ISOFFSET");
        sb.append("                    </").append(tag).append(">\n");
    }

    public static void createContract(String[] fields, List<String[]> lineItems) throws IOException, ParserConfigurationException, SAXException {
        // Required Fields
        StringBuilder sb = new StringBuilder("        <create_sotransaction>\n");
        appendTag(sb, fields[5], "transactiontype");
        appendDate(sb, fields[6], "datecreated");
        appendDate(sb, fields[8], "dateposted");
        appendTag(sb, fields[7], "customerid");
        appendTag(sb, fields[9], "documentno");
        appendTag(sb, fields[11], "referenceno");
        appendDate(sb, fields[10], "datedue");
        appendTag(sb, fields[12], "state");
        sb.append("          <sotransitems>\n");
        appendItems(sb, lineItems);
        sb.append("          </sotransitems>\n        </create_sotransaction>");

        String request = buildSageRequest(sb.toString());
        String response = postRequest(request, false);
        String message = getErrorMessage(response);
        Upload.appendTextArea("Contract with document number " + fields[9] + ": " + message);
    }

    public static void updateStandardCostCode(String id, String itemID, String billable, String[] categoryValues) throws IOException, ParserConfigurationException, SAXException {
        System.out.println("Updating standard cost code id: " + id);
        StringBuilder function = new StringBuilder();
        function.append("        <update>\n");
        function.append("          <STANDARDTASK>\n");
        function.append("            <STANDARDTASKID>").append(id).append("</STANDARDTASKID>\n");
        if (itemID != null && !itemID.isEmpty()) { function.append("            <ITEMID>").append(itemID).append("</ITEMID>\n"); }
        if (billable != null && !billable.isEmpty()) { function.append("            <BILLABLE>").append(billable).append("</BILLABLE>\n"); }
        function.append("            <STANDARDTASKSTANDARDCOSTTYPES>\n");

        for (String s : categoryValues) {
            if (s == null || s.isEmpty()) break;

            function.append("                <STANDARDTASKSTANDARDCOSTTYPE>\n");
            function.append("                    <STANDARDCOSTTYPEID>").append(s).append("</STANDARDCOSTTYPEID>\n");
            function.append("                </STANDARDTASKSTANDARDCOSTTYPE>\n");
        }

        function.append("            </STANDARDTASKSTANDARDCOSTTYPES>\n");
        function.append("          </STANDARDTASK>\n");
        function.append("        </update>\n");
        String request = buildSageRequest(function.toString());
        String response = postRequest(request, false);
        String message = getErrorMessage(response);

        Upload.appendTextArea("Update Item ID " + id + ": " + message);
    }
}
