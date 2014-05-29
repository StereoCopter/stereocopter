/*
 *  V4L2 video capture example
 *
 *  This program can be used and distributed without restrictions.
 *
 *      This program is provided with the V4L2 API
 * see http://linuxtv.org/docs.php for more information
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <getopt.h>             /* getopt_long() */

#include <fcntl.h>              /* low-level i/o */
#include <unistd.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/mman.h>
#include <sys/ioctl.h>

#include <linux/videodev2.h>

#define CLEAR(x) memset(&(x), 0, sizeof(x))

#ifndef V4L2_PIX_FMT_H264
#define V4L2_PIX_FMT_H264     v4l2_fourcc('H', '2', '6', '4') /* H264 with start codes */
#endif


typedef struct buffer {
        void   *start;
        size_t  length;
} buffer;

typedef struct cam_t {
	char            *dev_name;
	int              fd;
	struct buffer          *buffers;
	unsigned int     n_buffers;
	int              frame_number;
} cam_t;

cam_t* init_dev(const char* name) {
	cam_t* dev = calloc(1, sizeof(cam_t));
	dev->buffers = calloc(1, sizeof(*dev->buffers));
	dev->fd = -1;
	dev->dev_name=strdup(name);

	return dev;
}

void free_dev(cam_t* dev) {
	free(dev->dev_name);
	free(dev);
}

static void errno_exit(const char *s)
{
        fprintf(stderr, "%s error %d, %s\n", s, errno, strerror(errno));
        exit(EXIT_FAILURE);
}

static int xioctl(int fh, int request, void *arg)
{
        int r;

        do {
                r = ioctl(fh, request, arg);
        } while (-1 == r && EINTR == errno);

        return r;
}

static void process_image(cam_t* dev, const void *p, int size)
{
	printf("Cam: %d, Frame: %d\n", dev->fd, dev->frame_number);
        dev->frame_number++;
        char filename[32];
        sprintf(filename, "cam-%d-frame-%d.raw", dev->fd, dev->frame_number);
        FILE *fp=fopen(filename,"wb");

        fwrite(p, size, 1, fp);

        fflush(fp);
        fclose(fp);
}

static int read_frame(cam_t* dev)
{
        struct v4l2_buffer buf;
        unsigned int i;

                CLEAR(buf);

                buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                buf.memory = V4L2_MEMORY_MMAP;

                if (-1 == xioctl(dev->fd, VIDIOC_DQBUF, &buf)) {
                        switch (errno) {
                        case EAGAIN:
                                return 0;

                        case EIO:
                                /* Could ignore EIO, see spec. */

                                /* fall through */

                        default:
                                errno_exit("VIDIOC_DQBUF");
                        }
                }

                assert(buf.index < dev->n_buffers);

//		printf("mmap: %d, %08X, %d\n", buf.index, buffers[buf.index].start, buf.bytesused);
                process_image(dev, dev->buffers[buf.index].start, buf.bytesused);

                if (-1 == xioctl(dev->fd, VIDIOC_QBUF, &buf))
                        errno_exit("VIDIOC_QBUF");

        return 1;
}

static void mainloop(cam_t* dev)
{
        unsigned int count;

//        count = dev->frame_count;

	for(;;) {
                for (;;) {
                        fd_set fds;
                        struct timeval tv;
                        int r;

                        FD_ZERO(&fds);
                        FD_SET(dev->fd, &fds);

                        /* Timeout. */
                        tv.tv_sec = 20;
                        tv.tv_usec = 0;

                        r = select(dev->fd + 1, &fds, NULL, NULL, &tv);

                        if (-1 == r) {
                                if (EINTR == errno)
                                        continue;
                                errno_exit("select");
                        }

                        if (0 == r) {
                                fprintf(stderr, "select timeout\n");
                                exit(EXIT_FAILURE);
                        }

                        if (read_frame(dev))
                                break;
                        /* EAGAIN - continue select loop. */
                }
        }
}

static void stop_capturing(cam_t* dev)
{
        enum v4l2_buf_type type;

                type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                if (-1 == xioctl(dev->fd, VIDIOC_STREAMOFF, &type))
                        errno_exit("VIDIOC_STREAMOFF");
}

static void start_capturing(cam_t* dev)
{
        unsigned int i;
        enum v4l2_buf_type type;

                for (i = 0; i < dev->n_buffers; ++i) {
                        struct v4l2_buffer buf;

                        CLEAR(buf);
                        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                        buf.memory = V4L2_MEMORY_MMAP;
                        buf.index = i;

                        if (-1 == xioctl(dev->fd, VIDIOC_QBUF, &buf))
                                errno_exit("VIDIOC_QBUF");
                }
                type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                if (-1 == xioctl(dev->fd, VIDIOC_STREAMON, &type))
                        errno_exit("VIDIOC_STREAMON");
}

static void uninit_device(cam_t* dev)
{
        unsigned int i;

                for (i = 0; i < dev->n_buffers; ++i)
                        if (-1 == munmap(dev->buffers[i].start, dev->buffers[i].length))
                                errno_exit("munmap");

        free(dev->buffers);
}

static void init_read(cam_t* dev, unsigned int buffer_size)
{
       dev->buffers = calloc(1, sizeof(*dev->buffers));

        if (!dev->buffers) {
                fprintf(stderr, "Out of memory\n");
                exit(EXIT_FAILURE);
        }

        dev->buffers[0].length = buffer_size;
        dev->buffers[0].start = malloc(buffer_size);

        if (!dev->buffers[0].start) {
                fprintf(stderr, "Out of memory\n");
                exit(EXIT_FAILURE);
        }
}

static void init_mmap(cam_t* dev)
{
        struct v4l2_requestbuffers req;

        CLEAR(req);

        req.count = 4;
        req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        req.memory = V4L2_MEMORY_MMAP;

        if (-1 == xioctl(dev->fd, VIDIOC_REQBUFS, &req)) {
                if (EINVAL == errno) {
                        fprintf(stderr, "%s does not support "
                                 "memory mapping\n", dev->dev_name);
                        exit(EXIT_FAILURE);
                } else {
                        errno_exit("VIDIOC_REQBUFS");
                }
        }

        if (req.count < 2) {
                fprintf(stderr, "Insufficient buffer memory on %s\n",
                         dev->dev_name);
                exit(EXIT_FAILURE);
        }

        dev->buffers = calloc(req.count, sizeof(*dev->buffers));

        if (!dev->buffers) {
                fprintf(stderr, "Out of memory\n");
                exit(EXIT_FAILURE);
        }

        for (dev->n_buffers = 0; dev->n_buffers < req.count; ++dev->n_buffers) {
                struct v4l2_buffer buf;

                CLEAR(buf);

                buf.type        = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                buf.memory      = V4L2_MEMORY_MMAP;
                buf.index       = dev->n_buffers;

                if (-1 == xioctl(dev->fd, VIDIOC_QUERYBUF, &buf))
                        errno_exit("VIDIOC_QUERYBUF");

                dev->buffers[dev->n_buffers].length = buf.length;
                dev->buffers[dev->n_buffers].start =
                        mmap(NULL /* start anywhere */,
                              buf.length,
                              PROT_READ | PROT_WRITE /* required */,
                              MAP_SHARED /* recommended */,
                              dev->fd, buf.m.offset);

                if (MAP_FAILED == dev->buffers[dev->n_buffers].start)
                        errno_exit("mmap");
        }
}


static void init_device(cam_t* dev)
{
        struct v4l2_capability cap;
        struct v4l2_cropcap cropcap;
        struct v4l2_crop crop;
        struct v4l2_format fmt;
        unsigned int min;

        if (-1 == xioctl(dev->fd, VIDIOC_QUERYCAP, &cap)) {
                if (EINVAL == errno) {
                        fprintf(stderr, "%s is no V4L2 device\n",
                                 dev->dev_name);
                        exit(EXIT_FAILURE);
                } else {
                        errno_exit("VIDIOC_QUERYCAP");
                }
        }

        if (!(cap.capabilities & V4L2_CAP_VIDEO_CAPTURE)) {
                fprintf(stderr, "%s is no video capture device\n",
                         dev->dev_name);
                exit(EXIT_FAILURE);
        }

                if (!(cap.capabilities & V4L2_CAP_STREAMING)) {
                        fprintf(stderr, "%s does not support streaming i/o\n",
                                 dev->dev_name);
                        exit(EXIT_FAILURE);
                }


        /* Select video input, video standard and tune here. */


        CLEAR(cropcap);

        cropcap.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

        if (0 == xioctl(dev->fd, VIDIOC_CROPCAP, &cropcap)) {
                crop.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                crop.c = cropcap.defrect; /* reset to default */

                if (-1 == xioctl(dev->fd, VIDIOC_S_CROP, &crop)) {
                        switch (errno) {
                        case EINVAL:
                                /* Cropping not supported. */
                                break;
                        default:
                                /* Errors ignored. */
                                break;
                        }
                }
        } else {
                /* Errors ignored. */
        }


        CLEAR(fmt);

        fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

        fmt.fmt.pix.width       = 640; //replace
        fmt.fmt.pix.height      = 460; //replace
        fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_MJPEG; //replace
        fmt.fmt.pix.field       = V4L2_FIELD_ANY;

        if (-1 == xioctl(dev->fd, VIDIOC_S_FMT, &fmt))
        	errno_exit("VIDIOC_S_FMT");


        /* Buggy driver paranoia. */
        min = fmt.fmt.pix.width * 2;
        if (fmt.fmt.pix.bytesperline < min)
                fmt.fmt.pix.bytesperline = min;
        min = fmt.fmt.pix.bytesperline * fmt.fmt.pix.height;
        if (fmt.fmt.pix.sizeimage < min)
                fmt.fmt.pix.sizeimage = min;

                init_mmap(dev);
}

static void close_device(cam_t* dev)
{
        if (-1 == close(dev->fd))
                errno_exit("close");

        dev->fd = -1;
}

static void open_device(cam_t* dev)
{
        struct stat st;

        if (-1 == stat(dev->dev_name, &st)) {
                fprintf(stderr, "Cannot identify '%s': %d, %s\n",
                         dev->dev_name, errno, strerror(errno));
                exit(EXIT_FAILURE);
        }

        if (!S_ISCHR(st.st_mode)) {
                fprintf(stderr, "%s is no device\n", dev->dev_name);
                exit(EXIT_FAILURE);
        }

        dev->fd = open(dev->dev_name, O_RDWR /* required */ | O_NONBLOCK, 0);

        if (-1 == dev->fd) {
                fprintf(stderr, "Cannot open '%s': %d, %s\n",
                         dev->dev_name, errno, strerror(errno));
                exit(EXIT_FAILURE);
        }
}

static void* cam_thread(void* arg)
{
	cam_t* dev = init_dev((const char*)arg);

        open_device(dev);
        init_device(dev);
        start_capturing(dev);
        mainloop(dev);
        stop_capturing(dev);
        uninit_device(dev);
        close_device(dev);
        fprintf(stderr, "\n");

	free_dev(dev);

	return 0;
}

int main(int argc, char **argv)
{
	pthread_t cam0, cam1;


	pthread_create(&cam0, 0, cam_thread, "/dev/video0");
	pthread_create(&cam1, 0, cam_thread, "/dev/video1");

	pthread_join(cam0);
	pthread_join(cam1);

        return 0;
}
