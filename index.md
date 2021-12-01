# SCP (Secure Copy) MONITOR

You can use the [editor on GitHub](https://github.com/iii-org/scp-monitor/edit/gh-pages/index.md) to maintain and preview the content for your website in Markdown files.

Whenever you commit to this repository, GitHub Pages will run [Jekyll](https://jekyllrb.com/) to rebuild the pages in your site, from the content in your Markdown files.


## 使用情境
製造業者因資安考量，會將機台日誌檔案上傳至遠端特定資料夾，由資訊人員讀取檔案，進而分析數據。許多工廠是數百至數千個機台長期運轉，每日產出超大量的機台日誌檔案，亟需有效讀取檔案的輔助工具。SCP MONITOR改良一般開源軟體重複讀取所有檔案的模式，針對機台日誌檔案具有時間序的特性，使用本工具之應用程式，依所議定之時間間隔定時上傳機台日誌，能讀取最新且未處理過的檔案，並採用SFTP安全檔案傳遞協定，達到實時監控、安心讀檔。

## 解決問題
 1.	安全文件傳輸：日誌/檔案採用SFTP安全檔案傳輸協定，該協議使用SSH文件傳輸協議加密，提供可靠的網絡傳輸文件的安全方法
 2.	彈性設定：應用程式可透過設定檔名樣式讀取特定檔案的類型(.txt, .zip)，亦可配合當下基礎設施的限制設定每次讀取檔案之緩衝(buffer)，以避免因一次讀取大量檔案之記憶體不足之情形

## 主要功能
 1.	彈性實時監控：由應用程式依客戶端上傳的時間間隔定時觸發本工具讀取檔案
 2.	類訊息中介軟體：從檔名設定，實現檔案資料端到端 read only once（僅讀取一次），達到新舊檔案之辨識
 3.	讀取即紀錄：每一次讀取即紀錄，不重複讀取檔案，無須額外資料庫作為紀錄媒介

## 規格
### ScpMonitor屬性

<table>
    <tr>
        <td>屬性名</td>
        <td>型態</td>
        <td>用途</td>
    </tr>
    <tr>
        <td>prefix</td>
        <td>String</td>
        <td>欲監控檔案檔名之前綴詞</td>
    </tr>
    <tr>
        <td>postfix</td>
        <td>String</td>
        <td>欲監控檔案檔名之後綴詞</td>
    </tr>
    <tr>
        <td>accessPoints</td>
        <td>AccessPoint:List</td>
        <td>檔案源類別，ScpMonitor可接受多個遠端SFTP</td>
    </tr>
    <tr>
        <td>readbuffer</td>
        <td>Integer</td>
        <td>設定每次讀取檔案的大小(byte)</td>
    </tr>
    <tr>
        <td>daysToFind</td>
        <td>int[]</td>
        <td>用以當檔案無新增時設定往後幾日查找有無新增檔案，此設定主要是產線可能因停機或歲修無每日上拋日誌所設</td>
    </tr>
</table>

### 檔案源AccessPoint屬性

<table>
    <tr>
        <td>屬性名</td>
        <td>型態</td>
        <td>用途</td>
    </tr>
    <tr>
        <td>user</td>
        <td>String</td>
        <td>遠端SFTP帳號</td>
    </tr>
    <tr>
        <td>password</td>
        <td>String</td>
        <td>遠端SFTP密碼</td>
    </tr>
    <tr>
        <td>url</td>
        <td>String</td>
        <td>遠端FTP IP</td>
    </tr>
    <tr>
        <td>dir</td>
        <td>String[]</td>
        <td>欲監控之資料夾</td>
    </tr>
</table>

### 使用範例

  1. 產生物件ScpMonitor並提供前綴詞、日期樣式、後綴詞、read buffer做為建構子參數。
  2. 設定欲從哪個日期開始處理檔案(該日期樣式需與建構子之日期樣式一致)。
  3. 為物件ScpMonitor添加檔案源之設定。
  4. 重複呼叫method next()以讀取檔案。該next()回傳值為一Map物件，其中key為檔名，value則為二進位形式回傳之檔案。而若next()回傳空Map物件時則代表所指定之資料夾已經既有的檔案皆讀取完畢。

    
    (1)    ScpMonitor scpMonitor = new ScpMonitor("III_ARILOG_",  new SimpleDateFormat("yyyyMMdd"), "*.zip", 50000000);    
    (2)    scpMonitor.setFromDate("20211111");    
    (3)    scpMonitor.add(new AccessPoint("user", "passwd",  "192.168.1.100",  "/home/iii/III01" ));    
    scpMonitor.add(new AccessPoint("user", "passwd",  "192.168.1.101",  "/home/iii/III02" ));    
    try {    
    (4) 	  Map<String, byte[]> remoteFiles = scpMonitor.next();    
      System.out.println(remoteFiles.size());    
      for(String zip : remoteFiles.keySet()){    
      	System.out.println(zip + ": " + remoteFiles.get(zip).length);    
      }    
      
      remoteFiles = scpMonitor.next();    
      System.out.println(remoteFiles.size());    
      for(String zip : remoteFiles.keySet()){    
      	System.out.println(zip + ": " + remoteFiles.get(zip).length);    
      }    
	   } catch (Exception e) {    
		    // TODO Auto-generated catch block    
		    e.printStackTrace();    
    };    
    

### Markdown

Markdown is a lightweight and easy-to-use syntax for styling your writing. It includes conventions for

```markdown
Syntax highlighted code block

- Bulleted
- List

1. Numbered
2. List

**Bold** and _Italic_ and `Code` text

[Link](url) and ![Image](src)
```

For more details see [Basic writing and formatting syntax](https://docs.github.com/en/github/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax).

### Jekyll Themes

Your Pages site will use the layout and styles from the Jekyll theme you have selected in your [repository settings](https://github.com/iii-org/scp-monitor/settings/pages). The name of this theme is saved in the Jekyll `_config.yml` configuration file.

### Support or Contact

Having trouble with Pages? Check out our [documentation](https://docs.github.com/categories/github-pages-basics/) or [contact support](https://support.github.com/contact) and we’ll help you sort it out.
