using System.Windows.Forms;

namespace ConferenceTask
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
            Generator.GenerateMatrix();
        }

        private void button1_Click(object sender, System.EventArgs e)
        {
            var dispatcher = new Dispatcher(Generator.ReadMatrixFromFile());
            dispatcher.CreateShedule();
        }
    }
}
