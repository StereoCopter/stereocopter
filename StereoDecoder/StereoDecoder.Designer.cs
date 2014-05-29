namespace WindowsFormsApplication1
{
    partial class StereoDecoder
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.pb0 = new System.Windows.Forms.PictureBox();
            this.pb1 = new System.Windows.Forms.PictureBox();
            ((System.ComponentModel.ISupportInitialize)(this.pb0)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.pb1)).BeginInit();
            this.SuspendLayout();
            // 
            // pb0
            // 
            this.pb0.Location = new System.Drawing.Point(0, 0);
            this.pb0.Margin = new System.Windows.Forms.Padding(0);
            this.pb0.Name = "pb0";
            this.pb0.Size = new System.Drawing.Size(360, 640);
            this.pb0.TabIndex = 0;
            this.pb0.TabStop = false;
            // 
            // pb1
            // 
            this.pb1.Location = new System.Drawing.Point(360, 0);
            this.pb1.Name = "pb1";
            this.pb1.Size = new System.Drawing.Size(360, 640);
            this.pb1.TabIndex = 1;
            this.pb1.TabStop = false;
            // 
            // StereoDecoder
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(720, 640);
            this.Controls.Add(this.pb1);
            this.Controls.Add(this.pb0);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.None;
            this.Name = "StereoDecoder";
            this.Text = "Stereo Decoder!";
            ((System.ComponentModel.ISupportInitialize)(this.pb0)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.pb1)).EndInit();
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.PictureBox pb0;
        private System.Windows.Forms.PictureBox pb1;
    }
}

