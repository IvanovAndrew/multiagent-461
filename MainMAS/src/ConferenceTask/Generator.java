package ConferenceTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 18.01.14
 * Time: 16:58
 * To change this template use File | Settings | File Templates.
 */
public class Generator
{
    /**
     * количество слушателей
     */
    public static final int listeners = 10;
    /**
     * количество потоков
     */
    public static final int sections = 3;
    /**
     * количество докладов
     */
    public static final int reports = 30;
    /**
     * путь к файлу с входной матрицей
     */
    public static final String filePath = "input.txt";

    private static final int[][] matrix = new int[reports][listeners];

    public static final String separator = " ";

    /**
     * генерирует и записывает матрицу в файл
     */
    public static void generateMatrix ()
    {
        Random random = new Random();
        try
        {
            FileWriter writer = new FileWriter(filePath);
            for (int topic = 0; topic < reports; topic++)
            {
                for (int listener = 0; listener < listeners; listener++)
                {
                    writer.write(String.format("{0}", random.nextInt(10) + separator));
                }
                writer.write("\n");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    public static int[][] readMatrixFromFile (String fileName) throws IOException
    {
        if (fileName == null)
        {
            generateMatrix();
            fileName = filePath;
        }
        return readMatrixFromFile(new BufferedReader(new FileReader(fileName)));
    }

    /**
     * @return входная матрица из файла
     *         считывает матрицу из файла
     */
    public static int[][] readMatrixFromFile (BufferedReader input) throws IOException
    {
        String line = "";
        int matrix[][] = new int[reports][listeners];
        int lineNumber = 0;

        while ((line = input.readLine()) != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(line, separator);

            int i = 0;

            while (tokenizer.hasMoreTokens())
            {
                String token = tokenizer.nextToken();
                if (token.isEmpty())
                {
                    continue;
                }
                matrix[lineNumber][i++] = Integer.parseInt(token);
            }
            lineNumber++;
        }

        return matrix;
    }


    //		public static String generateName(java.util.Random random)
    //		{
    //			int length = random.nextInt(8);
    //			String name = "";
    //			for (int letter = 0; letter < length; letter++)
    //			{
    //				char ch = (char)('а' + random.nextInt(32));
    //				name += ch;
    //			}
    //			return name;
    //		}
}