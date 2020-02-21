package com.leeburch.poolstatus;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/status")
public class Status {
  TreeMap<Date, TreeMap<String, BigInteger>> usedData;
  TreeSet<String> allFileSystems;

  public Status() {
  }

  public static synchronized Date parseRFC3339Date(String dateString) throws ParseException, IndexOutOfBoundsException {
    Date d;
    if (dateString.endsWith("Z")) {
      try {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        s.setTimeZone(TimeZone.getTimeZone("UTC"));
        d = s.parse(dateString);
      } catch (ParseException var6) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
        s.setTimeZone(TimeZone.getTimeZone("UTC"));
        s.setLenient(true);
        d = s.parse(dateString);
      }

      return d;
    } else {
      String firstPart;
      String secondPart;
      if (dateString.lastIndexOf(43) == -1) {
        firstPart = dateString.substring(0, dateString.lastIndexOf(45));
        secondPart = dateString.substring(dateString.lastIndexOf(45));
      } else {
        firstPart = dateString.substring(0, dateString.lastIndexOf(43));
        secondPart = dateString.substring(dateString.lastIndexOf(43));
      }

      secondPart = secondPart.substring(0, secondPart.indexOf(58)) + secondPart.substring(secondPart.indexOf(58) + 1);
      dateString = firstPart + secondPart;
      SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.getDefault());

      try {
        d = s.parse(dateString);
      } catch (ParseException var7) {
        s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSZ", Locale.getDefault());
        s.setLenient(true);
        d = s.parse(dateString);
      }

      return d;
    }
  }

  @PostConstruct
  public void init() throws IOException {
    this.usedData = new TreeMap<>();
    this.allFileSystems = new TreeSet<>();
    Properties properties = new Properties();

    String scrubFile;
    scrubFile = null;

    InputStream stream = this.getClass().getResourceAsStream(this.getClass().getSimpleName() + ".properties");

    properties.load(stream);


    String dataFile = properties.getProperty("dataFile");
    scrubFile = properties.getProperty("history");

    try {
      BufferedReader in = new BufferedReader(new FileReader(dataFile));

      String line;
      String name;
      for (line = null; (line = in.readLine()) != null; this.allFileSystems.add(name)) {
        StringTokenizer st = new StringTokenizer(line, "\t");
        Date d = parseRFC3339Date(st.nextToken());
        name = st.nextToken();
        String type = st.nextToken();
        BigInteger used = new BigInteger(st.nextToken());
        TreeMap newUsedData;
        if (this.usedData.get(d) == null) {
          newUsedData = new TreeMap();
          newUsedData.put(name, used);
          this.usedData.put(d, newUsedData);
        } else {
          newUsedData = (TreeMap) this.usedData.get(d);
          newUsedData.put(name, used);
        }
      }

      Date firstUsedDate = (Date) this.usedData.keySet().iterator().next();
      in = new BufferedReader(new FileReader(scrubFile));
      SimpleDateFormat historyFormat = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss");

      while ((line = in.readLine()) != null) {
        Date d = historyFormat.parse(line.substring(0, line.indexOf(32)));
        if (d.after(firstUsedDate)) {
          TreeMap newUsedData;
          if (this.usedData.get(d) == null) {
            newUsedData = new TreeMap();
            newUsedData.put("scrub", new BigInteger("0"));
            this.usedData.put(d, newUsedData);
          } else {
            newUsedData = (TreeMap) this.usedData.get(d);
            newUsedData.put("scrub", new BigInteger("0"));
          }

          this.allFileSystems.add("scrub");
        }
      }
    } catch (FileNotFoundException fileNotFoundException) {
      fileNotFoundException.printStackTrace();
    } catch (IOException ioException) {
      ioException.printStackTrace();
    } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
      indexOutOfBoundsException.printStackTrace();
    } catch (ParseException parseException) {
      parseException.printStackTrace();
    }

    System.out.println("loaded " + this.usedData.size() + " dates");
  }

  @GET
  @Path("allsets")
  @Produces({"application/json"})
  public Object allSets() {
    return this.allFileSystems;
  }

  @GET
  @Path("useddata")
  @Produces({"application/json"})
  public Object usedData(@QueryParam("filesystem") List<String> ids) {
    Object[][] rv = new Object[this.usedData.keySet().size()][ids.size() + 1];
//     int i = 0;
    int j = 0;

    for (Iterator dateIterator = this.usedData.keySet().iterator(); dateIterator.hasNext(); ++j) {
      Date d = (Date) dateIterator.next();
      rv[j][0] = d;
      int i = 1;

      String key;
      for (Iterator fileSystemIterator = ids.iterator(); fileSystemIterator.hasNext(); rv[j][i++] = ((TreeMap) this.usedData.get(d)).get(key)) {
        key = (String) fileSystemIterator.next();
      }
    }

    return rv;
  }
}
