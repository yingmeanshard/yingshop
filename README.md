# YingShop

YingShop 是一個以 Spring 生態系為核心的線上購物網站範例，支援商品瀏覽、購物車、結帳、會員帳號管理與後台商品維護等流程。網站採用 Spring MVC + Hibernate 的三層架構設計，並透過 Spring Security 管理使用者登入與權限，搭配 Thymeleaf 模板產生前端頁面。

## 核心功能
- **商品瀏覽與分類**：依分類與上下架狀態顯示商品，並於後台提供搜尋、排序與分頁能力協助營運人員管理商品。【F:src/main/java/com/example/demo/controller/ProductController.java†L31-L88】【F:src/main/java/com/example/demo/controller/AdminController.java†L33-L137】
- **會員登入與權限控管**：整合 Spring Security，依角色決定允許的功能與登入後導向頁面，並預設建立管理員帳號。【F:src/main/java/com/example/demo/config/SecurityConfig.java†L19-L71】【F:src/main/java/com/example/demo/config/AdminAccountInitializer.java†L13-L27】
- **購物車與訂單流程**：提供購物車新增、調整、移除商品及結帳流程的業務邏輯，並將金額計算封裝於服務層。【F:src/main/java/com/example/demo/service/impl/CartServiceImpl.java†L15-L74】
- **圖片上傳與管理**：後台可一次上傳多張商品圖片，服務會驗證檔案、調整尺寸並儲存，同時維護封面設定與顯示順序。【F:src/main/java/com/example/demo/controller/AdminController.java†L138-L205】【F:src/main/java/com/example/demo/service/impl/FileUploadServiceImpl.java†L35-L164】
- **多語系與資源管理**：WebMvc 設定整合 Thymeleaf、Spring Security Dialect 及語系切換攔截器，並配置靜態資源路徑。【F:src/main/java/com/example/demo/config/WebMvcConfig.java†L18-L107】
- **日誌追蹤**：透過 Servlet Filter 為每個請求產生追蹤用的 Request ID，方便串接 Logback 觀測使用者操作。【F:src/main/java/yingshop/common/logging/RequestIdFilter.java†L8-L19】【F:src/main/resources/logback.xml†L5-L21】

## 技術與架構
- **Spring Framework 5**：使用 `spring-context`、`spring-webmvc`、`spring-orm` 等模組整合 MVC 與交易管理。【F:pom.xml†L19-L56】
- **Spring Security 5.7**：負責身份驗證、授權與登入流程客製化。【F:pom.xml†L24-L38】【F:src/main/java/com/example/demo/config/SecurityConfig.java†L19-L71】
- **Hibernate 5.6**：作為 ORM 實現，透過 DAO 層包裝資料存取，並支援 MySQL 與 H2 記憶體資料庫的自動切換。【F:pom.xml†L59-L77】【F:src/main/java/com/example/demo/config/HibernateConfig.java†L27-L90】
- **Thymeleaf 3**：渲染前端模板並結合 Spring Security 標籤與 Java 8 時間工具。【F:pom.xml†L40-L57】【F:src/main/java/com/example/demo/config/WebMvcConfig.java†L38-L73】
- **日誌系統**：採用 SLF4J + Logback，並於 `RequestIdFilter` 補強跨請求追蹤能力。【F:pom.xml†L105-L114】【F:src/main/java/yingshop/common/logging/RequestIdFilter.java†L8-L19】

專案採三層式架構 (Controller → Service → DAO)。控制器負責接收 HTTP 請求；服務層處理業務規則；DAO 層以 Hibernate `SessionFactory` 操作資料庫。【F:src/main/java/com/example/demo/controller/ProductController.java†L21-L114】【F:src/main/java/com/example/demo/service/impl/ProductServiceImpl.java†L13-L48】【F:src/main/java/com/example/demo/dao/impl/ProductDAOImpl.java†L13-L63】

DAO 的實作類別（如 `ProductDAOImpl`、`UserDAOImpl`）皆使用 Spring 的 `@Repository` 註解註冊為資料存取 Bean，統一封裝 Hibernate Session 操作，讓服務層僅需依賴介面即可切換或擴充儲存實作。【F:src/main/java/com/example/demo/dao/impl/ProductDAOImpl.java†L13-L68】【F:src/main/java/com/example/demo/dao/impl/UserDAOImpl.java†L12-L52】

### 商品列表分類篩選的實作流程
- **介面元件**：`products.html` 在列表上方放置 `<select name="category">` 下拉選單並以 GET 參數提交，預設提供「全部」與由後端傳入的分類集合，使用者選擇「上衣」等分類後按下套用即可觸發篩選。【F:src/main/webapp/WEB-INF/views/products.html†L63-L90】
- **控制器處理**：`ProductController#listProducts` 讀取 `category` 查詢參數並根據登入者身分決定資料來源；管理員呼叫 `ProductService#getProductsByCategory`，一般訪客則使用僅返回上架商品的 `getListedProductsByCategory`。同時會準備所有分類清單與目前選擇的分類回傳模板。【F:src/main/java/com/example/demo/controller/ProductController.java†L31-L72】
- **服務與資料層**：`ProductServiceImpl` 進一步將空白分類視為「全部」，否則呼叫 DAO。`ProductDAOImpl` 最終透過 Hibernate HQL `from Product where category = :category`（一般模式）或額外附帶 `listed = true` 條件的查詢，回傳符合的商品集合。【F:src/main/java/com/example/demo/service/impl/ProductServiceImpl.java†L31-L48】【F:src/main/java/com/example/demo/dao/impl/ProductDAOImpl.java†L33-L58】
- **呈現結果**：控制器將篩選後的商品與分類集合帶入模型，Thymeleaf 模板再以 `th:each` 迴圈呈現卡片。若沒有符合的商品則顯示提示訊息，以確保使用者明確知道篩選結果。【F:src/main/webapp/WEB-INF/views/products.html†L92-L154】

## 專案結構
```
yingshop/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/demo
│   │   │   ├── config/      # Spring、Hibernate、Security 設定
│   │   │   ├── controller/  # MVC 控制器
│   │   │   ├── dao/         # DAO 介面與實作
│   │   │   ├── model/       # JPA 實體與商業物件
│   │   │   ├── security/    # 自訂安全性策略
│   │   │   └── service/     # 業務服務介面與實作
│   │   ├── resources/       # 設定檔與國際化文案
│   │   └── webapp/          # Thymeleaf 模板與靜態資源
│   └── test/                # 單元與整合測試
└── README.md
```

## 系統需求
- JDK 11 以上
- Maven 3.8 以上
- MySQL 8（若 `application.properties` 未設定或連線失敗會自動改用內建 H2 記憶體資料庫）【F:pom.xml†L8-L13】【F:src/main/resources/application.properties†L1-L10】【F:src/main/java/com/example/demo/config/HibernateConfig.java†L33-L66】
- Servlet 容器（例如 Apache Tomcat 9）用於部署 WAR。【F:pom.xml†L6-L9】【F:pom.xml†L116-L136】

## 快速開始
1. 下載專案並匯入 IDE：
   ```bash
   git clone <repo-url>
   cd yingshop
   ```
2. 設定資料庫：修改 `src/main/resources/application.properties` 內的 MySQL 連線資訊。若保留空白，系統啟動時會自動切換為 H2 記憶體資料庫。【F:src/main/resources/application.properties†L1-L10】【F:src/main/java/com/example/demo/config/HibernateConfig.java†L33-L66】
3. 編譯並執行測試：
   ```bash
   mvn clean test
   ```
4. 打包 WAR：
   ```bash
   mvn clean package
   ```
   產生的 `target/yingshop.war` 可部署於 Tomcat 或其他 Servlet 容器。【F:pom.xml†L6-L9】【F:pom.xml†L116-L136】
5. 部署後造訪 `http://localhost:8080/yingshop/`，預設管理員帳號為 `ying / 55688`。【F:src/main/java/com/example/demo/config/AdminAccountInitializer.java†L19-L27】

## 測試
專案提供多個服務與資料層的單元測試、以及訂單流程的整合測試，可透過 Maven 指令執行：
```bash
mvn test
```
測試涵蓋訂單服務、購物車服務、使用者服務與驗證流程等關鍵場景。【F:src/test/java/com/example/demo/test/OrderServiceImplTest.java†L1-L169】【F:src/test/java/com/example/demo/test/CartServiceTest.java†L1-L129】【F:src/test/java/com/example/demo/integration/OrderPersistenceIntegrationTest.java†L1-L146】

## 常見問題
- **如何切換到記憶體資料庫？** 將 `application.properties` 的 `jdbc.url` 留空或移除，HibernateConfig 會自動偵測並建立 H2 資料庫，方便開發與測試。【F:src/main/java/com/example/demo/config/HibernateConfig.java†L33-L90】
- **登入後跳轉頁面可否自訂？** `RoleAwareAuthenticationSuccessHandler` 依使用者角色（Admin / Staff / 一般會員）決定導向頁面，可依需求調整對應 URL。【F:src/main/java/com/example/demo/security/RoleAwareAuthenticationSuccessHandler.java†L15-L38】

