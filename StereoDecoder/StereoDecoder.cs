﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Net.Sockets;
using System.Threading;
using System.Net;
using System.IO;

namespace WindowsFormsApplication1
{
    public partial class StereoDecoder : Form
    {
        const int DHT_SIZE = 420;
        const int HEADERFRAME1 = 0xaf;

        byte[] dht_data = new byte[] {
          0xff, 0xc4, 0x01, 0xa2, 0x00, 0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01,
          0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02,
          0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x01, 0x00, 0x03,
          0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
          0x0a, 0x0b, 0x10, 0x00, 0x02, 0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05,
          0x05, 0x04, 0x04, 0x00, 0x00, 0x01, 0x7d, 0x01, 0x02, 0x03, 0x00, 0x04,
          0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22,
          0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15,
          0x52, 0xd1, 0xf0, 0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17,
          0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x34, 0x35, 0x36,
          0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a,
          0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66,
          0x67, 0x68, 0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a,
          0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95,
          0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8,
          0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2,
          0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5,
          0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7,
          0xe8, 0xe9, 0xea, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9,
          0xfa, 0x11, 0x00, 0x02, 0x01, 0x02, 0x04, 0x04, 0x03, 0x04, 0x07, 0x05,
          0x04, 0x04, 0x00, 0x01, 0x02, 0x77, 0x00, 0x01, 0x02, 0x03, 0x11, 0x04,
          0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71, 0x13, 0x22,
          0x32, 0x81, 0x08, 0x14, 0x42, 0x91, 0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33,
          0x52, 0xf0, 0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25,
          0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x35, 0x36,
          0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a,
          0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66,
          0x67, 0x68, 0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a,
          0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92, 0x93, 0x94,
          0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
          0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba,
          0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
          0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7,
          0xe8, 0xe9, 0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa
        };


        public class CameraImage
        {
            public long ts;
            public int id, missing;
            public byte[] data;
        }

        CameraImage[] cams = new CameraImage[2];

        public void DrawImage(CameraImage img)
        {
            try
            {
            if (img.missing != 0)
                return;

            /*
            memcpy(vd->tmpbuffer, vd->mem[vd->buf.index], HEADERFRAME1);
            memcpy(vd->tmpbuffer + HEADERFRAME1, dht_data, DHT_SIZE);
            memcpy(vd->tmpbuffer + HEADERFRAME1 + DHT_SIZE,
                    vd->mem[vd->buf.index] + HEADERFRAME1,
                    (vd->buf.bytesused - HEADERFRAME1));

            */
            byte[] jpeg = new byte[img.data.Length + DHT_SIZE];

            //[0 ... HEADERFRAME1) -> [0 ... HEADERFRAME1)
            Array.Copy(img.data, 0, jpeg, 0, HEADERFRAME1);

            //[0 ... DHT_SIZE) -> [HEADERFRAME1 ... HEADERFRAME1 + DHT_SIZE]
            Array.Copy(dht_data, 0, jpeg, HEADERFRAME1, DHT_SIZE);

            //[HEADERFRAME1 ... end] -> [HEADERFRAME1 + DHT_SIZE ... end]

            Array.Copy(img.data, HEADERFRAME1, jpeg, HEADERFRAME1 + DHT_SIZE, img.data.Length - HEADERFRAME1);
            var ms = new MemoryStream(jpeg);

            
                var bmp = Image.FromStream(ms);

                bmp.RotateFlip(RotateFlipType.Rotate90FlipNone);
                (img.id == cswi ? pb0 : pb1).BackgroundImage = bmp;
            }
            catch (Exception ex) { }
        }

        UdpClient d;
        public StereoDecoder()
        {
            InitializeComponent();

            d = new UdpClient(9001);


            d.EnableBroadcast = true;
            d.Client.EnableBroadcast = true;

            Thread rxt = new Thread(() =>
            {
                IPEndPoint iep = new IPEndPoint(0, 0);

                for (; ; )
                {
                    try
                    {
                        var data = d.Receive(ref iep);

                        int p = 0;

                        int camid = BitConverter.ToInt32(data, p); p += 4;
                        int ts_s = BitConverter.ToInt32(data, p); p += 4;
                        int ts_ns = BitConverter.ToInt32(data, p); p += 4;
                        int slice_size = BitConverter.ToInt32(data, p); p += 4;
                        int sn = BitConverter.ToInt32(data, p); p += 4;
                        int sc = BitConverter.ToInt32(data, p); p += 4;
                        int total = BitConverter.ToInt32(data, p); p += 4;
                        int len = BitConverter.ToInt32(data, p); p += 4;

                        long ts = ts_s + (ts_ns * 1000000);
                        byte[] payload = data.Skip(p).ToArray();

                        if (payload.Length != len)
                            continue;

                        var cam = cams[camid];

                        if (cam == null || cam.ts < ts)
                        {
                            if (cam != null)
                                this.BeginInvoke(new ThreadStart(() => this.DrawImage(cam)));

                            cam = cams[camid] = new CameraImage();

                            cam.id = camid;
                            cam.ts = ts;
                            cam.data = new byte[total];
                            cam.missing = sc;
                        }

                        Array.Copy(payload, 0, cam.data, sn * slice_size, len);
                        cam.missing--;

                        if (cam.missing == 0)
                        {
                            this.BeginInvoke(new ThreadStart(() => this.DrawImage(cam)));
                            cams[camid] = null;
                        }
                    }
                    catch (Exception) { }

                }
            });

            rxt.Start();
        }

        private void pb1_Click(object sender, EventArgs e)
        {

        }

        private void pb1_DoubleClick(object sender, EventArgs e)
        {
            if (WindowState == FormWindowState.Maximized)
                WindowState = FormWindowState.Normal;
            else
                WindowState = FormWindowState.Maximized;

        }

        int xo = 0;
        int yo = 0;
        int cswi = 0;

        void repos()
        {
            int p0x = Width * 1 / 4 + xo;
            int p0y = Height * 1 / 2 + yo;

            int p1x = Width * 3 / 4 - xo;
            int p1y = Height * 1 / 2 + yo;

            pb0.Left = p0x - pb0.Width / 2;
            pb0.Top = p0y - pb0.Height / 2;

            pb1.Left = p1x - pb1.Width / 2;
            pb1.Top = p1y - pb1.Height / 2;
        }

        private void StereoDecoder_Resize(object sender, EventArgs e)
        {
            repos();
        }

        private void StereoDecoder_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Left)
                xo--;
            else if (e.KeyCode == Keys.Right)
                xo++;

            if (e.KeyCode == Keys.Up)
                yo--;
            else if (e.KeyCode == Keys.Down)
                yo++;

            if (e.KeyCode == Keys.Space)
                cswi ^= 1;

            if (e.KeyCode == Keys.Escape)
                pb1_DoubleClick(null, null);

            repos();
        }
    }
}
