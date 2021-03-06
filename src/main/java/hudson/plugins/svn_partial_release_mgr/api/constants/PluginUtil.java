package hudson.plugins.svn_partial_release_mgr.api.constants;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import org.tmatesoft.svn.core.SVNURL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.UserInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class PluginUtil {
  public static final Properties configurationProperties = loadConfigurationProperties();

  /**
   * Reads a property list (key and element pairs) from the resources
   */
  private static Properties loadConfigurationProperties() {
    InputStream fis = null;
    try {
      fis = PluginUtil.class.getResourceAsStream("/" + Constants.CONFIGURATION_PROPERTIES_FILE);
      if (fis == null) {
        return null;
      }
      Properties applicationProperties = new Properties();
      applicationProperties.load(fis);
      return applicationProperties;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  /**
   * Returns a string configuration value that has been set for this configuration key
   *
   * @param property : the name of the configuration key we are looking for
   * @return the configuration value for this key
   */
  public static String getConfiguration(String property) {
    return configurationProperties != null ? configurationProperties.getProperty(property) : null;
  }

  public static String pathToTag(SVNURL svnurl,
                                 String tagName) {
    return pathToTagWithSvnUrl(svnurl.toDecodedString(), tagName);
  }

  public static String pathToTagWithSvnUrl(String svnurlFullPath,
                                           String tagName) {
    String pathToTag = pathToTag("trunk", svnurlFullPath, tagName);
    if (!StringUtils.isBlank(pathToTag)) {
      return pathToTag;
    }
    return pathToTag("branches", svnurlFullPath, tagName);
  }

  protected static String pathToTag(String prefix,
                                    String fullPath,
                                    String tagName) {
    if (!fullPath.contains(prefix)) {
      return null;
    }
    fullPath = fullPath.substring(0, fullPath.lastIndexOf(prefix));
    fullPath = fullPath + Constants.DIR_NAME_TAGS + "/" + tagName;
    return fullPath;
  }

  public static void log(TaskListener listener,
                         String message) {
    String dateString = "[" +
        DateFormatUtils.format(new Date(), Constants.DEPLOY_DATE_FORMAT) + "]";
    listener.getLogger().println(dateString + " " + Constants.LOG_PREFIX + message);
  }

  public static String getWorkspaceDeploymentPath(String workspaceRootPath,
                                                  String tagName) {
    return FilenameUtils.separatorsToUnix(workspaceRootPath +
        "/" + Constants.DIR_NAME_DEPLOYMENTS + "/" + tagName);
  }

  public static String getWorkspaceBuildRootPath(String workspaceRootPath,
                                                 String tagName) {
    return FilenameUtils.separatorsToUnix(workspaceRootPath +
        "/" + Constants.DIR_NAME_BUILDS + "/" + tagName);
  }

  public static String getFullPathToTagBackupSource(String workspaceRootPath,
                                                    String tagName) {
    return FilenameUtils.separatorsToUnix(workspaceRootPath + "/" +
        getRelativePathToTagBackupSource(tagName));
  }

  public static String getRelativePathToTagBackupSource(String tagName) {
    return Constants.DIR_NAME_CHECKOUT + "/" + Constants.DIR_NAME_TAGS + "/" + tagName;
  }

  public static String getWorkspaceTagDeploymentDatePath(FilePath workspace,
                                                         TagDeploymentInfo tagDeploymentInfo) throws
      IOException {
    String deploymentWorkspaceDirPath = PluginUtil.getWorkspaceDeploymentPath(workspace.getRemote(),
        tagDeploymentInfo.getTagName());
    String dirDateName = DateFormatUtils.format(parse(tagDeploymentInfo.getDeploymentDate(),
        Constants.DEPLOY_DATE_FORMAT), Constants.DEPLOY_DATE_FORMAT_FILE);
    return FilenameUtils.separatorsToUnix(deploymentWorkspaceDirPath +
        "/" + dirDateName);
  }

  public static String getWorkspaceTagBuildRootDirectory(FilePath workspace,
                                                         TagDeploymentInfo tagDeploymentInfo) throws
      IOException {
    return PluginUtil.getWorkspaceBuildRootPath(workspace.getRemote(),
        tagDeploymentInfo.getTagName());
  }

  public static Date parse(String dateString,
                           String dateFormat) throws IOException {
    try {
      return new SimpleDateFormat(dateFormat).parse(dateString);
    } catch (ParseException e) {
      throw new IOException("Could not parse the date [" + dateString + "] "
          + "with format [" + dateFormat + "]");
    }
  }

  /**
   * Creates a new empty document object
   *
   * @return the newly created document
   */
  public static Document buildNewW3CDocument() {
    DocumentBuilderFactory factory =
        DocumentBuilderFactory.newInstance();
    Document document = null;
    try {
      DocumentBuilder builder =
          factory.newDocumentBuilder();
      document = builder.newDocument();
    } catch (ParserConfigurationException pce) {
      // Parser with specified options can't be built
      pce.printStackTrace();
    }
    return document;
  }

  /**
   * Reads the content of the xml file into a new document object
   *
   * @param file the file to get the xml from
   * @return the newly created document
   */
  public static Document buildW3CDocumentFromFile(File file,
                                                  String encoding) throws Exception {
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setValidating(false);
    DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
    LineNumberReader lnr = null;
    Document doc = null;
    try {
      lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(file), encoding));
      InputSource inSource = new InputSource(lnr);
      doc = domBuilder.parse(inSource);
    } finally {
      if (lnr != null) {
        try {
          //System.out.println("Closing " + filePath);
          lnr.close();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
    return doc;
  }

  /**
   * Returns the value of a child tag with the given name ( if exists) inside this element
   *
   * @param element  : the given XML Element object
   * @param tagChild : the name of the child tag
   * @return the text value in string ( in CDATA element or simple one )
   */
  public static String getValueFromChildElement(Node element,
                                                String tagChild) {
    Node valueNode = findNextNode(element, tagChild);
    return valueNode != null ? getValueFromTextNode(valueNode) : null;
  }

  /**
   * Reads all child tags and returns them as a java.util.Properties object
   *
   * @param element : the XML Element object
   * @return a Properties object with all tag name/value pairs
   */
  public static Map<String, String> getChildNodeValues(Node element) {
    if (element == null) {
      return null;
    }
    if (!element.hasChildNodes()) {
      return null;
    }
    Map<String, String> result = new LinkedHashMap<>();
    NodeList nl = element.getChildNodes();
    if (nl == null || nl.getLength() <= 0) {
      return null;
    }
    for (int i = 0; i < nl.getLength(); i++) {
      Node nd = nl.item(i);
      String xmlValue = getValueFromTextNode(nd);
      if (!StringUtils.isBlank(xmlValue)) {
        result.put(nd.getNodeName(), xmlValue);
      }
    }
    return result;
  }

  /**
   * Creates a list of the child elements with the input tag inside the given element
   *
   * @param node the element to get the child elements of
   * @param name the tag name to get elements for
   * @return a list of the child elements with the input tag inside the given element
   */
  public static List<Node> getNodesInNode(Node node,
                                          String name) {
    if (node == null) {
      return null;
    }
    if (!node.hasChildNodes()) {
      return null;
    }
    NodeList childrenNodes = node.getChildNodes();
    if (childrenNodes == null || childrenNodes.getLength() <= 0) {
      return null;
    }
    List<Node> nodes = null;
    for (int i = 0; i < childrenNodes.getLength(); i++) {
      Node nd = childrenNodes.item(i);
      if (nd != null && nd.getNodeName() != null && nd.getNodeName().equals(name)) {
        if (nodes == null) {
          nodes = new ArrayList<Node>();
        }
        nodes.add(nd);
      }
    }
    return nodes;
  }

  /**
   * Returns the first child element with the input tag name inside the given element
   *
   * @param node    : the target xml Element
   * @param tagName : the tag name to search element for
   * @return the first found Element
   */
  public static Node findNextNode(Node node,
                                  String tagName) {
    if (node == null) {
      return null;
    }
    if (!node.hasChildNodes()) {
      return null;
    }
    NodeList childrenNodes = node.getChildNodes();
    if (childrenNodes == null || childrenNodes.getLength() <= 0) {
      return null;
    }
    Node _retNode = null;
    for (int i = 0; i < childrenNodes.getLength(); i++) {
      Node nd = childrenNodes.item(i);
      if (nd.getNodeName().equals(tagName)) {
        return nd;
      } else {
        _retNode = findNextNode(nd, tagName);
      }
    }
    return _retNode;
  }

  /**
   * Returns the text value of the given element
   *
   * @param element : the given XML Element object
   * @return the text value in string
   */
  public static String getValueFromTextNode(Node element) {
    if (element == null) {
      return null;
    }
    NodeList childrenNodes = element.getChildNodes();
    if (childrenNodes == null || childrenNodes.getLength() <= 0) {
      return null;
    }
    StringBuilder text = null;
    for (int i = 0; i < childrenNodes.getLength(); i++) {
      Node childNode = childrenNodes.item(i);
      if (childNode == null) {
        continue;
      }
      if (Node.TEXT_NODE == childNode.getNodeType()) {
        if (text == null) {
          text = new StringBuilder();
        }
        text.append(childNode.getNodeValue());
      }
    }
    return text != null ? text.toString() : null;
  }


  /**
   * Creates a new child element with the given tag name inside the input element and appends the input value in it
   *
   * @param rootNode    the node to create a child element into
   * @param elementName the name of the tag of the new child element that will be created
   * @param value       the value to set to the newly created tag
   * @return the newly created child element
   */
  public static Node addNodeInNode(Node rootNode,
                                   String elementName,
                                   String value) {
    Document document = rootNode.getOwnerDocument();
    Element newElement = document.createElement(elementName);
    setNodeValue(newElement, value);
    rootNode.appendChild(newElement);
    return newElement;
  }

  /**
   * Sets a new text value in this element
   *
   * @param element   : the target xml Element
   * @param textValue : the text value to be entered inside the Element
   */
  public static void setNodeValue(Node element,
                                  String textValue) {
    if (StringUtils.isBlank(textValue)) {
      return;
    }
    if (element == null) {
      return;
    }
    if (element.hasChildNodes()) {
      Node vFirstChild = element.getFirstChild();
      if (vFirstChild.getNodeType() == Node.TEXT_NODE) {
        vFirstChild.setNodeValue(textValue);
        return;
      }
      if (vFirstChild.getNodeType() == Node.CDATA_SECTION_NODE) {
        element.removeChild(vFirstChild);
        Document doc = element.getOwnerDocument();
        element.appendChild(doc.createTextNode(textValue));
      }
      return;
    }
    Document doc = element.getOwnerDocument();
    element.appendChild(doc.createTextNode(textValue));
  }

  /**
   * Stores the xml document into the input file
   *
   * @param document the input XML Document object
   * @param file     the file to be stored the input document
   */
  public static void toFile(Document document,
                            File file) throws Exception {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      toOutputStream(document, fos);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   * Stores the xml document into the input file
   *
   * @param document the input XML Document object
   * @param out      the OutputStream to be stored the input document
   */
  public static <O extends OutputStream> O toOutputStream(Document document,
                                                          O out) throws Exception {
    transform(document, Constants.XML_OUTPUT_PROPERTIES, new StreamResult(out));
    return out;
  }

  /**
   * Main transformation method. It transform the input document using the input properties
   * and returns the result into the input streamResult
   *
   * @param document         the input XML Document object
   * @param outputProperties the properties to use for output
   * @param streamResult     the object to be stored the transformation result
   */
  public static void transform(Document document,
                               Properties outputProperties,
                               StreamResult streamResult) throws Exception {
    TransformerFactory transFactory = TransformerFactory.newInstance();
    Transformer transformer = transFactory.newTransformer();
    if (outputProperties != null) {
      Enumeration<?> propertyNames = outputProperties.propertyNames();
      while (propertyNames.hasMoreElements()) {
        String propName = propertyNames.nextElement().toString();
        String propValue = outputProperties.getProperty(propName);
        if (propValue == null) {
          continue;
        }
        transformer.setOutputProperty(propName, propValue);
      }
    }
    transformer.transform(new DOMSource(document), streamResult);
  }

  /**
   * Returns all the files that satisfy the filter criteria wherever (in any replace-directory) they are inside this folder
   *
   * @param parentDir : the parent directory
   * @param filter    : the files filter
   * @return a list of all the available files
   */
  public static List<File> getAllFilesInAllSubDirectories(File parentDir,
                                                          FileFilter filter) {
    List<File> filesList = null;
    return getAllFilesInAllSubDirectories(filesList, parentDir, filter);
  }

  /**
   * Returns all the files that satisfy the filter criteria wherever (in any replace-directory) they are inside this folder
   *
   * @param filesList : the list to attach the files into
   * @param parentDir : the parent directory
   * @param filter    : the files filter
   * @return a list of all the available files
   */
  public static List<File> getAllFilesInAllSubDirectories(List<File> filesList,
                                                          File parentDir,
                                                          FileFilter filter) {
    if (parentDir == null || !parentDir.exists()) {
      return filesList;
    }
    File[] files = parentDir.listFiles(filter);
    if (files != null) {
      for (File vFile : files) {
        if (filesList == null) {
          filesList = new ArrayList<>();
        }
        filesList.add(vFile);
      }
    }
    File[] directories = parentDir.listFiles();
    if (directories != null) {
      for (File vDirectory : directories) {
        filesList = getAllFilesInAllSubDirectories(filesList, vDirectory, filter);
      }
    }
    return filesList;
  }

  /**
   * Puts an object to an inner Set of the given Map ( initializes it first if null )
   *
   * @param mapValues : the Map to insert the object into
   * @param key       : the key under which the object is to be inserted
   * @param obj       : the Object to be inserted
   * @return the updated Map
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, Set<V>> putCheckedObjectInInnerSet(Map<K, Set<V>> mapValues,
                                                                 K key,
                                                                 V obj) {
    Set<V> innerSet = null;
    if (mapValues != null && mapValues.containsKey(key)) {
      innerSet = mapValues.get(key);
    }
    innerSet = addCheckedObjectInSet(innerSet, obj);
    mapValues = addCheckedObjectInMap(mapValues, key, innerSet);
    return mapValues;
  }

  /**
   * Adds an object to a given Set ( initializes it first if null )
   *
   * @param existingObjectsSet : the Set to insert the object into
   * @param obj                : the Object to be inserted
   * @return the updated list
   */
  @SuppressWarnings("unchecked")
  public static <T, O extends T> Set<T> addCheckedObjectInSet(Set<T> existingObjectsSet,
                                                              O obj) {
    if (obj != null) {
      if (existingObjectsSet == null) {
        existingObjectsSet = new HashSet<>();
      }
      existingObjectsSet.add(obj);
    }
    return existingObjectsSet;
  }

  /**
   * Puts an object to a given Map ( initializes it first if null )
   *
   * @param mapValues : the Map to insert the object into
   * @param key       : the key under which the object is to be inserted
   * @param obj       : the Object to be inserted
   * @return the updated Map
   */
  @SuppressWarnings("unchecked")
  public static <K, V, O extends V> Map<K, V> addCheckedObjectInMap(Map<K, V> mapValues,
                                                                    K key,
                                                                    O obj) {
    if (obj != null) {
      if (mapValues == null) {
        mapValues = new HashMap<>();
      }
      mapValues.put(key, obj);
    }
    return mapValues;
  }

  /**
   * Puts an object to an inner Map of the given Map ( initializes it first if null )
   *
   * @param mapValues : the Map to insert the object into
   * @param key       : the key under which the object is to be inserted
   * @param innerKey  : the key under which the object is to be inserted
   * @param obj       : the Object to be inserted
   * @return the updated Map
   */
  @SuppressWarnings("unchecked")
  public static <A, K, V> Map<A, Map<K, V>> putCheckedObjectInInnerMap(Map<A, Map<K, V>> mapValues,
                                                                       A key,
                                                                       K innerKey,
                                                                       V obj) {
    Map<K, V> innerMap = null;
    if (mapValues != null && mapValues.containsKey(key)) {
      innerMap = mapValues.get(key);
    }
    innerMap = addCheckedObjectInMap(innerMap, innerKey, obj);
    mapValues = addCheckedObjectInMap(mapValues, key, innerMap);
    return mapValues;
  }

  /**
   * Flag to check if we should make the job disabled
   *
   * @return true if the job has been started
   */
  public static boolean isJobStarted() {
    String systemFlag = System.getProperty(Constants.SYSTEM_PROPERTY_BUILD_STARTED);
    return Boolean.TRUE.toString().equalsIgnoreCase(systemFlag);
  }

  /**
   * Flag to check if we should make the job disabled
   */
  public static void setJobStarted() {
    System.setProperty(Constants.SYSTEM_PROPERTY_BUILD_STARTED,
        String.valueOf(Boolean.TRUE));
  }

  /**
   * Flag to check if we should make the job disabled
   */
  public static void setJobEnded() {
    System.setProperty(Constants.SYSTEM_PROPERTY_BUILD_STARTED,
        String.valueOf(Boolean.FALSE));
  }

  /**
   * Returns the method declared in the given class or in any superclass
   *
   * @param clazz       : the class object to execute the method of
   * @param methodName  : the method name of the class object to execute
   * @param classParams : the parameter objects array
   * @return the reflect method outorge object
   */
  public static Method getReflectionMethodInAnySuperClass(Class clazz,
                                                          String methodName,
                                                          Class[] classParams) {
    Method mainMethod = getClassMethod(clazz, methodName, classParams);
    while (mainMethod == null) {
      Class superClass = clazz.getSuperclass();
      mainMethod = getClassMethod(superClass, methodName, classParams);
    }
    return mainMethod;
  }

  /**
   * Returns the method declared in the given class ( and not in any superclass )
   *
   * @param clazz       : the class object to execute the method of
   * @param methodName  : the method name of the class object to execute
   * @param classParams : the parameter objects array
   * @return the reflect method outorge object
   */
  protected static Method getClassMethod(Class clazz,
                                         String methodName,
                                         Class[] classParams) {
    try {
      return clazz.getDeclaredMethod(methodName, classParams);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * Calls a method of a given object by reflection
   *
   * @param instanceOfClass : the class object to execute the method of
   * @param methodName      : the method name of the class object to execute
   * @param paramClasses    : the parameter classes array
   * @param paramObjects    : the parameter objects array
   * @return the method outorge object
   */
  public static Object reflectionCallMethod(Object instanceOfClass,
                                            String methodName,
                                            Class[] paramClasses,
                                            Object[] paramObjects) throws Exception {
    return reflectionCallMethod(instanceOfClass, methodName, paramClasses, paramObjects, true);
  }

  /**
   * Calls a method of a given object by reflection
   *
   * @param instanceOfClass : the class object to execute the method of
   * @param methodName      : the method name of the class object to execute
   * @param paramClasses    : the parameter classes array
   * @param paramObjects    : the parameter objects array
   * @return the method outorge object
   */
  public static Object reflectionCallMethod(Object instanceOfClass,
                                            String methodName,
                                            Class[] paramClasses,
                                            Object[] paramObjects,
                                            boolean searchSuperClasses) throws Exception {
    // look up its main(String[]) method
    Method mainMethod = searchSuperClasses ?
        getReflectionMethodInAnySuperClass(instanceOfClass.getClass(), methodName, paramClasses) :
        getClassMethod(instanceOfClass.getClass(), methodName, paramClasses);
    if (mainMethod == null) {
      throw new NoSuchMethodException("Error! " + instanceOfClass.getClass().getName() + " " +
          "class does not contain a [" + methodName + "] method.");
    }
    // run the main(String[]) method with the given arguments
    return mainMethod.invoke(instanceOfClass, paramObjects);
  }

  /**
   * Compares a string against another one after eliminating the null pointer exception
   *
   * @param s      the String to be compared
   * @param target the String to be compared against
   * @return the cropped String
   */
  public static boolean isEqualsSafe(String s,
                                     String target) {
    return isEqualsSafe(s, target, false);
  }

  /**
   * Compares a string against another one after eliminating the null pointer exception
   *
   * @param s          the String to be compared
   * @param target     the String to be compared against
   * @param ignoreCase if true the equal comparator will ignore the case
   * @return the cropped String
   */
  public static boolean isEqualsSafe(String s,
                                     String target,
                                     boolean ignoreCase) {
    if (s == null && target == null) {
      return true;
    }
    if (s == null) {
      return false;
    }
    return ignoreCase ? s.equalsIgnoreCase(target) : s.equals(target);
  }

  public static boolean isGeneratePartialPatch(UserInput userInput) {
    return isBooleanAdditionParameterTrue(userInput,
        Constants.ENV_PARAM_GENERATE_PARTIAL_PATCH);
  }

  public static boolean isGenerateSourcePartialPatch(UserInput userInput) {
    return isBooleanAdditionParameterTrue(userInput,
        Constants.ENV_PARAM_GENERATE_SRC_PARTIAL_PATCH);
  }

  public static boolean isGeneratePatchForEveryIssue(UserInput userInput) {
    return isBooleanAdditionParameterTrue(userInput,
        Constants.ENV_PARAM_GENERATE_PATCH_FOR_EVERY_ISSUE);
  }

  public static boolean isFastBuild(UserInput userInput) {
    return isBooleanAdditionParameterTrue(userInput,
        Constants.ENV_PARAM_IS_FAST_BUILD);
  }

  public static boolean isTestBuild(UserInput userInput) {
    return isBooleanAdditionParameterTrue(userInput,
        Constants.ENV_PARAM_IS_TEST_BUILD);
  }

  public static boolean isIncludePreviousPatchSources(UserInput userInput) {
    return isBooleanAdditionParameterTrue(userInput,
        Constants.ENV_PARAM_INCLUDE_PREV_PATCH_SOURCES);
  }

  /**
   * Checkbox value for checked in UI will be set to 1 so we compare against 1
   *
   * @param userInput     the user input values wrapper
   * @param attributeName the attribute name to check if it has been checked the checkbox for
   * @return true if the checkbox has been checked
   */
  private static boolean isBooleanAdditionParameterTrue(UserInput userInput,
                                                        String attributeName) {
    String value = userInput.getAdditionalParameterValue(attributeName);
    return NumberUtils.toInt(value) == 1;
  }
}
