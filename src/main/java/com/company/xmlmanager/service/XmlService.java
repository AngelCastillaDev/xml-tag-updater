
package com.company.xmlmanager.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

@Service
public class XmlService {

    public void copiarBloqueXml(String rutaOrigen, String rutaDestino) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document docOrigen = builder.parse(new File(rutaOrigen));
        Document docDestino = builder.parse(new File(rutaDestino));

        NodeList nodos = docOrigen.getElementsByTagNameNS("*", "AdditionalDocumentReference");
        if (nodos.getLength() == 0)
            throw new Exception("No se encontró el nodo <AdditionalDocumentReference>");

        Node nodoCopiado = docDestino.importNode(nodos.item(0), true);

        NodeList notas = docDestino.getElementsByTagNameNS("*", "Note");
        if (notas.getLength() == 0)
            throw new Exception("No se encontró <cbc:Note>");

        Node nota = notas.item(0);
        Node padre = nota.getParentNode();
        Node siguiente = nota.getNextSibling();

        if (siguiente != null) padre.insertBefore(nodoCopiado, siguiente);
        else padre.appendChild(nodoCopiado);

        File archivoSalida = new File("src/main/resources/output", new File(rutaDestino).getName());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(docDestino), new StreamResult(archivoSalida));
    }

    public void insertarDocumentoAdicional(String rutaOrigen, String rutaDestino, String nuevoID) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document docOrigen = builder.parse(new File(rutaOrigen));
        Document docDestino = builder.parse(new File(rutaDestino));

        NodeList nodos = docOrigen.getElementsByTagNameNS("*", "AdditionalDocumentReference");
        Node nodoSeleccionado = null;

        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            String id = e.getElementsByTagNameNS("*", "ID").item(0).getTextContent();
            if (id.contains("VarDocumentAdditional")) {
                nodoSeleccionado = e;
                break;
            }
        }

        if (nodoSeleccionado == null)
            throw new Exception("No se encontró el nodo con ID VarDocumentAdditional");

        Node nodoCopiado = docDestino.importNode(nodoSeleccionado, true);
        NodeList hijos = ((Element) nodoCopiado).getElementsByTagNameNS("*", "ID");
        if (hijos.getLength() > 0) {
            Node idNode = hijos.item(0);
            while (idNode.hasChildNodes()) idNode.removeChild(idNode.getFirstChild());
            CDATASection cdata = docDestino.createCDATASection(nuevoID);
            idNode.appendChild(cdata);
        }

        NodeList notas = docDestino.getElementsByTagNameNS("*", "Note");
        if (notas.getLength() == 0)
            throw new Exception("No se encontró <cbc:Note>");

        Node nota = notas.item(0);
        Node padre = nota.getParentNode();
        Node siguiente = nota.getNextSibling();

        if (siguiente != null) padre.insertBefore(nodoCopiado, siguiente);
        else padre.appendChild(nodoCopiado);

        File archivoSalida = new File("src/main/resources/output", new File(rutaDestino).getName());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(docDestino), new StreamResult(archivoSalida));
    }

    public void copiarDesdeFuenteYEditar(File archivoFuente, File archivoDestino, String nuevoID, String otrosDatos) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document docFuente = builder.parse(archivoFuente);
        Document docDestino = builder.parse(archivoDestino);

        // Buscar <AdditionalDocumentReference> con ID que contenga "VarDocumentAdditional"
        NodeList nodos = docFuente.getElementsByTagNameNS("*", "AdditionalDocumentReference");
        Node nodoEncontrado = null;

        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            String id = e.getElementsByTagNameNS("*", "ID").item(0).getTextContent().trim();
            if (id.contains("VarDocumentAdditional")) {
                nodoEncontrado = e;
                break;
            }
        }

        if (nodoEncontrado == null)
            throw new Exception("No se encontró <AdditionalDocumentReference> con ID VarDocumentAdditional");

        // Copiar y modificar el nodo
        Node nodoCopiado = docDestino.importNode(nodoEncontrado, true);
        NodeList hijos = ((Element) nodoCopiado).getElementsByTagNameNS("*", "ID");

        if (hijos.getLength() > 0) {
            Node idNode = hijos.item(0);
            idNode.setTextContent("");
            CDATASection cdata = docDestino.createCDATASection(nuevoID);
            idNode.appendChild(cdata);
        }

        // Buscar <cbc:Note> con texto 'Obs a la GRE'
        NodeList notas = docDestino.getElementsByTagNameNS("*", "Note");
        Node notaTarget = null;

        for (int i = 0; i < notas.getLength(); i++) {
            if (notas.item(i).getTextContent().contains("Obs a la GRE")) {
                notaTarget = notas.item(i);
                break;
            }
        }

        if (notaTarget == null)
            throw new Exception("No se encontró <cbc:Note> con contenido 'Obs a la GRE'");

        Node padre = notaTarget.getParentNode();
        Node siguiente = notaTarget.getNextSibling();

        // Insertar el nodo copiado
        if (siguiente != null) padre.insertBefore(nodoCopiado, siguiente);
        else padre.appendChild(nodoCopiado);

        // Crear comentario XML <!-- AUTORIZACIONES ESPECIALES -->
        Comment comentario = docDestino.createComment(" AUTORIZACIONES ESPECIALES ");

        // Crear el bloque <PartyLegalEntity> con <CompanyID>
        Element partyLegalEntity = docDestino.createElementNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
                "cac:PartyLegalEntity"
        );

        Element companyId = docDestino.createElementNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
                "cbc:CompanyID"
        );
        companyId.appendChild(docDestino.createCDATASection(otrosDatos));
        partyLegalEntity.appendChild(companyId);

        // Insertar el comentario y el bloque después del nodo copiado
        if (siguiente != null) {
            padre.insertBefore(comentario, siguiente);
            padre.insertBefore(partyLegalEntity, siguiente);
        } else {
            padre.appendChild(comentario);
            padre.appendChild(partyLegalEntity);
        }

        // Guardar cambios en el archivo destino
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(docDestino), new StreamResult(archivoDestino));
    }

}
