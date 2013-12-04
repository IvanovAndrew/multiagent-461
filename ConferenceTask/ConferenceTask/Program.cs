using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows.Forms;

namespace ConferenceTask
{
    static class Program
    {
        static void Main()
        {
            Generator.GenerateMatrix();
            var testMatrix = Generator.ReadMatrixFromFile();
        }
    }
}
