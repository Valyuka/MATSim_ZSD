package org.matsim.run;

import com.google.common.collect.Table;

import java.io.FileWriter;
import java.io.IOException;

public class WriteToCSV {

    public static void run(Table table, String file, boolean appendToFile) {

        String path = file;

        try {
            FileWriter writer;
            writer = new FileWriter(path, appendToFile);

            // Write CSV
            writer.write(",");
            for (int i = 0; i<=23; i++) {
                writer.write(String.valueOf(i));
                if (i<23) {
                    writer.write(",");
                }
            }

            for (Object row:table.rowKeySet()) {
                writer.write("\r\n");
                writer.write(String.valueOf(row));
                writer.write(",");
                for (int i = 0; i <= 23; i++) {
                    if (table.columnKeySet().contains(i) && table.get(row,i)!=null)  {
                        writer.write(String.valueOf(table.get(row, i)));
                        writer.write(",");
                    } else {
                        writer.write("0");
                        writer.write(",");
                    }
                }
            }

            System.out.println("Write success!");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
