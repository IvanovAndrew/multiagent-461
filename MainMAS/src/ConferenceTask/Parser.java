package ConferenceTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew
 * Date: 02.01.14
 * Time: 22:44
 * To change this template use File | Settings | File Templates.
 */
public class Parser
{
    public final static int reports = 6;
    public final static int listeners = 4;
    public final static int sections = 3;

    public static int[][] parse (BufferedReader input) throws IOException
    {
        String line = "";
        int matrix[][] = new int[reports][listeners];
        int lineNumber = 0;

        while ((line = input.readLine()) != null)
        {
            StringTokenizer tokenizer = new StringTokenizer (line, " ");

            int i = 0;

            while (tokenizer.hasMoreTokens ())
            {
                String token = tokenizer.nextToken ();
                if (token.isEmpty ())
                {
                    continue;
                }
                matrix[lineNumber][i++] = Integer.parseInt (token);
            }

            lineNumber++;
        }

        return matrix;
    }
}
