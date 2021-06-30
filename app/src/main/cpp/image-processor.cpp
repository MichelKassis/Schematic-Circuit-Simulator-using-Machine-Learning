#include <jni.h>
#include <stdio.h>
#include <queue>
#include <cmath>
#include "macros.h"

struct Image {
    int width;
    int height;

    int bytes_per_pixel;

    unsigned char *bitmap = NULL;

    int width_in_bytes;
    int num_bytes;
};

enum Direction {
    N, NE, E, SE, S, SW, W, NW
};

struct Link {
    Link *next = NULL;
    Link *previous = NULL;
    int x, y;
    Direction dir = N;
};

struct Chain {
    Link *head = NULL;
    Link *tail = NULL;
    int length = 0;
};

struct Box {
    int left, right, top, bottom;
    bool is_label = false;
};

struct ProcessingResult {
    int num_boxes;
    Box *boxes;

    int num_connections;
    int *connections;
    int *connections_with_id;
    int *connections_with_sides;
    int ground_node;
};

#define PERCENT 1.0f

#define MIN_BOX_SIZE 3000

#define MAX_BOXES 1024
#define MAX_CHAINS 4096

Box boxes[MAX_BOXES];
int num_boxes = 0;

Chain chains[MAX_CHAINS];
int num_chains = 0;

int *connection_matrix = NULL;
int *connection_matrix_with_id = NULL;
int *connection_matrix_with_sides = NULL;

int num_connections;

int ground_node = -1;

Image bw_test_image;
Image chains_test_image;
Image dtt_test_image;
Image dtt_labels_test_image;
Image components_test_image;
Image labels_test_image;
Image dtot_components_test_image;
Image dtot_labels_test_image;
Image connections_test_image;
Image result_test_image;

/* BEGIN JNI CODE */

extern "C" JNIEXPORT jstring JNICALL
Java_ca_mcgill_schematicreader_interfaces_JNIImageProcessor_stringFromJNI(JNIEnv *env,
                                                                          jobject thiz) {
#if defined(__arm__)
#if defined(__ARM_ARCH_7A__)
#if defined(__ARM_NEON__)
#if defined(__ARM_PCS_VFP)
#define ABI "armeabi-v7a/NEON (hard-float)"
#else
#define ABI "armeabi-v7a/NEON"
#endif
#else
#if defined(__ARM_PCS_VFP)
#define ABI "armeabi-v7a (hard-float)"
#else
#define ABI "armeabi-v7a"
#endif
#endif
#else
#define ABI "armeabi"
#endif
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
#define ABI "mips64"
#elif defined(__mips__)
#define ABI "mips"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#else
#define ABI "unknown"
#endif

    return env->NewStringUTF("Hello from JNI !  Compiled with ABI " ABI ".");
}

ProcessingResult do_image_processing(Image test_image);

float guess_bw_threshold(Image image);

Image do_bw_conversion(Image image, float threshold);

// API function for image processing.
extern "C" JNIEXPORT jobject JNICALL
Java_ca_mcgill_schematicreader_interfaces_JNIImageProcessor_process(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height,
        jint bytes_per_pixel,
        jbyteArray bitmap,
        jint width_in_bytes,
        jint num_bytes) {
    int byteArrayLength = env->GetArrayLength(bitmap);
    unsigned char *buf = new unsigned char[byteArrayLength];
    env->GetByteArrayRegion(bitmap, 0, byteArrayLength, reinterpret_cast<jbyte *>(buf));

    Image image;
    image.width = width;
    image.height = height;
    image.bytes_per_pixel = bytes_per_pixel;
    image.bitmap = buf;
    image.width_in_bytes = width_in_bytes;
    image.num_bytes = num_bytes;
    ProcessingResult pr = do_image_processing(image);

    jclass complexClass = env->FindClass("ca/mcgill/schematicreader/model/ProcessingResult");
    jmethodID constructor = env->GetMethodID(complexClass, "<init>", "()V");
    jobject instance = env->NewObject(complexClass, constructor);

    jclass boxClass = env->FindClass("ca/mcgill/schematicreader/model/Box");
    jmethodID boxConstructor = env->GetMethodID(boxClass, "<init>", "()V");
    jobjectArray boxArray = env->NewObjectArray(pr.num_boxes, boxClass, nullptr);

    jfieldID topField = env->GetFieldID(boxClass, "top", "I");
    jfieldID leftField = env->GetFieldID(boxClass, "left", "I");
    jfieldID bottomField = env->GetFieldID(boxClass, "bottom", "I");
    jfieldID rightField = env->GetFieldID(boxClass, "right", "I");

    for (int i = 0; i < pr.num_boxes; ++i) {
        jobject boxObject = env->NewObject(boxClass, boxConstructor);

        env->SetIntField(boxObject, topField, pr.boxes[i].top);
        env->SetIntField(boxObject, leftField, pr.boxes[i].left);
        env->SetIntField(boxObject, bottomField, pr.boxes[i].bottom);
        env->SetIntField(boxObject, rightField, pr.boxes[i].right);

        env->SetObjectArrayElement(boxArray, i, boxObject);
    }

    jfieldID boxesField = env->GetFieldID(complexClass, "boxes", "I");
    jfieldID connectionsField = env->GetFieldID(complexClass, "connections", "I");
    jfieldID boxArrayField = env->GetFieldID(complexClass, "boxArray",
                                             "[Lca/mcgill/schematicreader/model/Box;");
    jfieldID groundNodeField = env->GetFieldID(complexClass, "groundNode", "I");
    jfieldID connectionMatrixWithIdField = env->GetFieldID(complexClass, "connectionMatrixWithId",
                                                           "[I");
    jfieldID connectionMatrixWithSidesField = env->GetFieldID(complexClass,
                                                              "connectionMatrixWithSides",
                                                              "[I");

    int matrixSize = pr.num_connections * pr.num_connections;

    jintArray connectionMatrixWithId = env->NewIntArray(matrixSize);

    jint temporaryMatrix[matrixSize];

    for (int i = 0; i < matrixSize; ++i) {
        temporaryMatrix[i] = connection_matrix_with_id[i];
    }

    env->SetIntArrayRegion(connectionMatrixWithId, 0, matrixSize, temporaryMatrix);

    jintArray connectionMatrixWithSides = env->NewIntArray(matrixSize);

    jint temporaryMatrix2[matrixSize];

    for (int i = 0; i < matrixSize; ++i) {
        temporaryMatrix2[i] = connection_matrix_with_sides[i];
    }

    env->SetIntArrayRegion(connectionMatrixWithSides, 0, matrixSize, temporaryMatrix2);

    env->SetIntField(instance, boxesField, pr.num_boxes);
    env->SetIntField(instance, connectionsField, pr.num_connections);
    env->SetObjectField(instance, boxArrayField, boxArray);
    env->SetIntField(instance, groundNodeField, pr.ground_node);
    env->SetObjectField(instance, connectionMatrixWithIdField, connectionMatrixWithId);
    env->SetObjectField(instance, connectionMatrixWithSidesField, connectionMatrixWithSides);

    return instance;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_ca_mcgill_schematicreader_interfaces_JNIImageProcessor_doBwConversion(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height,
        jint bytes_per_pixel,
        jbyteArray bitmap,
        jint width_in_bytes,
        jint num_bytes,
        jfloat threshold) {
    int byteArrayLength = env->GetArrayLength(bitmap);
    unsigned char *buf = new unsigned char[byteArrayLength];
    env->GetByteArrayRegion(bitmap, 0, byteArrayLength, reinterpret_cast<jbyte *>(buf));

    Image image;
    image.width = width;
    image.height = height;
    image.bytes_per_pixel = bytes_per_pixel;
    image.bitmap = buf;
    image.width_in_bytes = width_in_bytes;
    image.num_bytes = num_bytes;

    Image bw = do_bw_conversion(image, threshold);

    jbyteArray arr = env->NewByteArray(byteArrayLength);
    env->SetByteArrayRegion(arr, 0, byteArrayLength, (jbyte *) bw.bitmap);

    return arr;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_ca_mcgill_schematicreader_interfaces_JNIImageProcessor_guessBwThreshold(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height,
        jint bytes_per_pixel,
        jbyteArray bitmap,
        jint width_in_bytes,
        jint num_bytes) {
    int byteArrayLength = env->GetArrayLength(bitmap);
    unsigned char *buf = new unsigned char[byteArrayLength];
    env->GetByteArrayRegion(bitmap, 0, byteArrayLength, reinterpret_cast<jbyte *>(buf));

    Image image;
    image.width = width;
    image.height = height;
    image.bytes_per_pixel = bytes_per_pixel;
    image.bitmap = buf;
    image.width_in_bytes = width_in_bytes;
    image.num_bytes = num_bytes;

    return guess_bw_threshold(image);
}

/* END JNI CODE */

int max(int a, int b) { return (a > b) ? a : b; }

int min(int a, int b) { return (a < b) ? a : b; }

void reset() {
    num_boxes = 0;
    num_chains = 0;

    if (connection_matrix) free(connection_matrix);
    connection_matrix = NULL;

    if (connection_matrix_with_id) free(connection_matrix_with_id);
    connection_matrix_with_id = NULL;

    if (connection_matrix_with_sides) free(connection_matrix_with_sides);
    connection_matrix_with_sides = NULL;

    num_connections = 0;

    ground_node = -1;
}

void rgb_to_bgr(Image *image) {
    for (int i = 0; i < image->num_bytes; i += image->bytes_per_pixel) {
        char temp = image->bitmap[i];
        image->bitmap[i + 0] = image->bitmap[i + 2];
        image->bitmap[i + 2] = temp;
    }
}

float guess_bw_threshold(Image image) {
    reset();

    double average_weight = 0;

    for (int i = 0; i < image.num_bytes; i += 4) {
        float grey = 255.0f;
        for (int j = 0; j < 3; j++) {
            grey = fmin(grey, image.bitmap[j + i]);
        }

        average_weight += grey / 255;
    }

    average_weight /= (image.width * image.height);
    return 0.8f * average_weight;
}

// This thing assumes 32-bit depth, which is what we have (mostly due to drawing constraint on Windows, but be careful still.
void convert_to_bw(Image *image, Image *result, float threshold) {
    result->width = image->width;
    result->height = image->height;
    result->bytes_per_pixel = 4;
    result->bitmap = (unsigned char *) malloc(
            result->width * result->height * result->bytes_per_pixel);
    result->width_in_bytes = result->width * result->bytes_per_pixel;
    result->num_bytes = result->width * result->height * result->bytes_per_pixel;

    for (int i = 0; i < image->num_bytes; i += 4) {
        float grey = 255.0f;
        for (int j = 0; j < 3; j++) {
            grey = fmin(grey, image->bitmap[j + i]);
        }

        grey /= 255;

        if (grey > threshold) {
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 0] = 255;
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 1] = 255;
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 2] = 255;
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 3] = 255;
        } else {
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 0] = 0;
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 1] = 0;
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 2] = 0;
            result->bitmap[i / image->bytes_per_pixel * result->bytes_per_pixel + 3] = 255;
        }
    }
}

void do_the_thing(Image *image, Image *result, int sq_size, float weight_threshold) {
    result->width = image->width;
    result->height = image->height;
    result->bytes_per_pixel = 4;
    result->bitmap = (unsigned char *) malloc(
            result->width * result->height * result->bytes_per_pixel);
    result->width_in_bytes = result->width * result->bytes_per_pixel;
    result->num_bytes = result->width * result->height * result->bytes_per_pixel;

    for (int i = 0; i < image->width - sq_size; i++) {
        for (int j = 0; j < image->height - sq_size; j++) {
            int offset = (j * image->width + i) * 4;
            int local_weight = 0;
            for (int y = 0; y < sq_size; y++) {
                int row_offset = offset + (y * image->width) * 4;
                for (int x = 0; x < sq_size; x++) {
                    local_weight += image->bitmap[row_offset + x * 4];
                }
            }

            float average_weight = ((float) local_weight) / (sq_size * sq_size);

            if (average_weight > weight_threshold) {
                for (int y = 0; y < sq_size; y++) {
                    int row_offset = offset + (y * image->width) * 4;
                    for (int x = 0; x < sq_size; x++) {
                        result->bitmap[row_offset + x * 4] = 255;
                    }
                }
            }
        }
    }
}


float blue_percentage(Image *image, Box *box) {
    int width = box->right - box->left + 1;
    int height = box->bottom - box->top + 1;

    float weight = 0.0;

    for (int i = 0; i < width; i++) {
        int x = (box->left + i) * 4;
        for (int j = 0; j < height; j++) {
            int y = box->top + j;
            weight += image->bitmap[x + (y * image->width_in_bytes)];
        }
    }

    return weight / (255 * width * height);
}

bool inside_box(int x, int y) {
    for (int i = 0; i < num_boxes; i++) {
        if (x >= boxes[i].left && x <= boxes[i].right &&
            y >= boxes[i].top && y <= boxes[i].bottom) {
            return true;
        }
    }

    return false;
}

float box_size(Box *box) {
    return (float) (box->right - box->left) * (box->bottom - box->top);
}

bool try_expand_right(Image *image, Box *box) {
    if (box->right >= image->width - 1) return false;
    box->right += 1;
    if (blue_percentage(image, box) < PERCENT) {
        box->right -= 1;
        return false;
    }
    return true;
}

bool try_expand_bottom(Image *image, Box *box) {
    if (box->bottom >= image->height - 1) return false;
    box->bottom += 1;
    if (blue_percentage(image, box) < PERCENT) {
        box->bottom -= 1;
        return false;
    }
    return true;
}

bool try_expand_left(Image *image, Box *box) {
    if (box->left <= 0) return false;
    box->left -= 1;
    if (blue_percentage(image, box) < PERCENT) {
        box->left += 1;
        return false;
    }
    return true;
}

bool try_expand_top(Image *image, Box *box) {
    if (box->top <= 0) return false;
    box->top -= 1;
    if (blue_percentage(image, box) < PERCENT) {
        box->top += 1;
        return false;
    }
    return true;
}

float box_overlap(Box *b1, Box *b2) {
    if (b1->top > b2->bottom || b2->top > b1->bottom ||
        b1->left > b2->right || b2->left > b1->right) {
        return 0.0f;
    }

    Box intersect;
    intersect.top = max(b1->top, b2->top);
    intersect.bottom = min(b1->bottom, b2->bottom);
    intersect.left = max(b1->left, b2->left);
    intersect.right = min(b1->right, b2->right);

    return fmax(box_size(&intersect) / box_size(b1), box_size(&intersect) / box_size(b2));
}

void draw_boxes(Image *image, int start_index, int stop_index) {
    for (int box_index = start_index; box_index < stop_index; box_index++) {
        Box *box = &boxes[box_index];
        // draw top line
        for (int x = box->left; x <= box->right; x++) {
            int y = box->top;
            image->bitmap[(x + y * image->width) * 4 + 0] = 255;
            image->bitmap[(x + y * image->width) * 4 + 1] = 0;
            image->bitmap[(x + y * image->width) * 4 + 2] = 0;
        }

        // draw bottom line
        for (int x = box->left; x <= box->right; x++) {
            int y = box->bottom;
            image->bitmap[(x + y * image->width) * 4 + 0] = 255;
            image->bitmap[(x + y * image->width) * 4 + 1] = 0;
            image->bitmap[(x + y * image->width) * 4 + 2] = 0;
        }

        // draw left line
        for (int y = box->top; y <= box->bottom; y++) {
            int x = box->left;
            image->bitmap[(x + y * image->width) * 4 + 0] = 255;
            image->bitmap[(x + y * image->width) * 4 + 1] = 0;
            image->bitmap[(x + y * image->width) * 4 + 2] = 0;
        }

        // draw right line
        for (int y = box->top; y <= box->bottom; y++) {
            int x = box->right;
            image->bitmap[(x + y * image->width) * 4 + 0] = 255;
            image->bitmap[(x + y * image->width) * 4 + 1] = 0;
            image->bitmap[(x + y * image->width) * 4 + 2] = 0;
        }

        // Draw inside
        for (int x = box->left + 1; x < box->right; x++) {
            for (int y = box->top + 1; y < box->bottom; y++) {

                if (box->is_label) {
                    image->bitmap[(x + y * image->width) * 4 + 0] =
                            255 * ((float) image->bitmap[(x + y * image->width) * 4 + 0]) / 255;
                    image->bitmap[(x + y * image->width) * 4 + 1] =
                            230 * ((float) image->bitmap[(x + y * image->width) * 4 + 1]) / 255;
                    image->bitmap[(x + y * image->width) * 4 + 2] =
                            230 * ((float) image->bitmap[(x + y * image->width) * 4 + 2]) / 255;
                } else {
                    image->bitmap[(x + y * image->width) * 4 + 0] =
                            230 * ((float) image->bitmap[(x + y * image->width) * 4 + 0]) / 255;
                    image->bitmap[(x + y * image->width) * 4 + 1] =
                            230 * ((float) image->bitmap[(x + y * image->width) * 4 + 1]) / 255;
                    image->bitmap[(x + y * image->width) * 4 + 2] =
                            255 * ((float) image->bitmap[(x + y * image->width) * 4 + 2]) / 255;
                }

            }
        }
    }
}

void find_boxes(Image *image, Image *result, Image *orig) {
    // Go through every pixel
    // If blue and not in box, start box
    // Push right as much as possible
    // Push down as much as possible
    // Draw borders

    int previous_num_boxes = num_boxes;

    result->width = image->width;
    result->height = image->height;
    result->bytes_per_pixel = 4;
    result->bitmap = (unsigned char *) malloc(
            result->width * result->height * result->bytes_per_pixel);
    result->width_in_bytes = result->width * result->bytes_per_pixel;
    result->num_bytes = result->width * result->height * result->bytes_per_pixel;

    memcpy(result->bitmap, orig->bitmap, result->num_bytes);

    for (int i = 0; i < image->height; i++) {
        for (int j = 0; j < image->width; j++) {
            unsigned char pixel_b = image->bitmap[(j + i * image->width) * 4];
            if (pixel_b == 255) {
                if (!inside_box(j, i)) {
                    Box box;
                    box.left = j;
                    box.right = j;
                    box.top = i;
                    box.bottom = i;

                    bool expanded = true;
                    while (expanded) {
                        expanded = false;
                        expanded |= try_expand_right(image, &box);
                        expanded |= try_expand_bottom(image, &box);
                        expanded |= try_expand_left(image, &box);
                        expanded |= try_expand_top(image, &box);
                    }

                    //if(box_size(&box) > MIN_BOX_SIZE) {

                    if (num_boxes >= MAX_BOXES) { return; }

                    boxes[num_boxes++] = box;

                    //printf("Added box %d (%d, %d) (%d, %d, %d, %d)\n", num_boxes, j, i, box.left, box.top, box.right, box.bottom);

                    //}
                }
            }
        }
    }

    // Merge boxes
    for (int i = previous_num_boxes; i < num_boxes; i++) {
        for (int j = i + 1; j < num_boxes; j++) {
            if (i == j) continue;
            if (box_overlap(&boxes[i], &boxes[j]) > 0.05f) {
                // Merge
                boxes[i].left = min(boxes[i].left, boxes[j].left);
                boxes[i].right = max(boxes[i].right, boxes[j].right);
                boxes[i].top = min(boxes[i].top, boxes[j].top);
                boxes[i].bottom = max(boxes[i].bottom, boxes[j].bottom);

                boxes[j] = boxes[num_boxes - 1];
                num_boxes--;

                i = previous_num_boxes;
                j = i;
            }
        }
    }
}

#define MIN_CHAIN_BB_SURFACE 2500

bool isBoundary(int x, int y, Image *image) {
    if (image->bitmap[(x + y * image->width) * 4] == 0) {
        if (x < image->width - 1) {
            if (image->bitmap[((x + 1) + y * image->width) * 4] == 255) {
                return true;
            }
        }
        if (x > 1) {
            if (image->bitmap[((x - 1) + y * image->width) * 4] == 255) {
                return true;
            }
        }

        if (y < image->height - 1) {
            if (image->bitmap[(x + (y + 1) * image->width) * 4] == 255) {
                return true;
            }
        }
        if (y > 1) {
            if (image->bitmap[(x + (y - 1) * image->width) * 4] == 255) {
                return true;
            }
        }
    }

    return false;
}

void maybeMoveNextValidLink(int x, int y, Direction d, Link *next_link, Link *link, Image *image,
                            Image *result) {
    if (x == -1 || x == result->width || y == -1 || y == result->height) return;
    if (result->bitmap[(x + y * image->width) * 4] == 255) return;
    if (isBoundary(x, y, image)) {
        next_link->previous = link;
        link->next = next_link;
        next_link->x = x;
        next_link->y = y;
        next_link->dir = d;
        result->bitmap[(x + y * image->width) * 4] = 255;
    }
}

void CW(Direction *d) {
    int value = ((int) *d) + 1;
    *d = (Direction) value;
}

// TODO, makes this handle circular list
void reverse_chain(Chain *chain) {
    struct Link *temp = NULL;
    struct Link *current = chain->head;

    /* swap next and prev for all nodes of
      doubly linked list */
    while (current != NULL) {
        temp = current->previous;
        current->previous = current->next;
        current->next = temp;
        current = current->previous;
    }

    temp = chain->head;
    chain->head = chain->tail;
    chain->tail = temp;
}

int link_distance2(Link *a, Link *b) {
    float dx = a->x - b->x;
    float dy = a->y - b->y;
    return dx * dx + dy * dy;
}

int chain_BB_surface(Chain chain) {
    int left = chain.head->x;
    int right = chain.head->x;
    int top = chain.head->y;
    int bottom = chain.head->y;
    Link *link = chain.head->next;
    for (int i = 1; i < chain.length; i++) {
        if (link->x < left) left = link->x;
        if (link->x > right) right = link->x;
        if (link->y < top) top = link->y;
        if (link->y > bottom) bottom = link->y;
        link = link->next;
    }

    return (right - left + 1) * (bottom - top + 1);
}

#define CHAIN_MERGE_DISTANCE2 25

void find_chains(Image *image, Image *result) {
    result->width = image->width;
    result->height = image->height;
    result->bytes_per_pixel = 4;
    result->bitmap = (unsigned char *) malloc(
            result->width * result->height * result->bytes_per_pixel);
    result->width_in_bytes = result->width * result->bytes_per_pixel;
    result->num_bytes = result->width * result->height * result->bytes_per_pixel;
    memset(result->bitmap, 0, result->num_bytes);

    for (int i = 0; i < image->height; i++) {
        for (int j = 0; j < image->width; j++) {
            unsigned char pixel_b = image->bitmap[(j + i * image->width) * 4];
            unsigned char result_pixel_b = result->bitmap[(j + i * image->width) * 4];
            if (pixel_b == 0 && result_pixel_b != 255) {
                if (isBoundary(j, i, image)) {
                    Link *head = (Link *) malloc(sizeof(Link));
                    head->next = NULL;
                    head->previous = NULL;
                    head->x = j;
                    head->y = i;
                    head->dir = N;
                    Chain chain;
                    chains[num_chains].head = head;
                    chains[num_chains].length = 1;
                    Link *link = head;
                    result->bitmap[(link->x + link->y * image->width) * 4] = 255;
                    do {
                        Link *next_link = (Link *) malloc(sizeof(Link));
                        next_link->next = NULL;
                        next_link->previous = link;
                        Direction d = link->dir;
                        for (int k = 0; k < 8; k++) {

                            int offset = ((k + 1) / 2) * ((k % 2 == 0) ? 1 : -1);
                            Direction d = (Direction) (((int) link->dir + offset + 8) % 8);

                            if (d == N)
                                maybeMoveNextValidLink(link->x, link->y - 1, d, next_link, link,
                                                       image, result);
                            else if (d == NW)
                                maybeMoveNextValidLink(link->x - 1, link->y - 1, d, next_link, link,
                                                       image, result);
                            else if (d == SW)
                                maybeMoveNextValidLink(link->x - 1, link->y + 1, d, next_link, link,
                                                       image, result);
                            else if (d == W)
                                maybeMoveNextValidLink(link->x - 1, link->y, d, next_link, link,
                                                       image, result);
                            else if (d == NE)
                                maybeMoveNextValidLink(link->x + 1, link->y - 1, d, next_link, link,
                                                       image, result);
                            else if (d == E)
                                maybeMoveNextValidLink(link->x + 1, link->y, d, next_link, link,
                                                       image, result);
                            else if (d == SE)
                                maybeMoveNextValidLink(link->x + 1, link->y + 1, d, next_link, link,
                                                       image, result);
                            else if (d == S)
                                maybeMoveNextValidLink(link->x, link->y + 1, d, next_link, link,
                                                       image, result);

                            if (link->next) break;
                        }

                        if (link->next == NULL) {
                            free(next_link);
                            break;
                        } else {
                            result->bitmap[(next_link->x + next_link->y * image->width) * 4] = 255;
                            chains[num_chains].length++;
                            link = next_link;
                        }

                    } while (true);
                    chains[num_chains].tail = link;
                    num_chains++;
                }
            }
        }
    }

    // reset result
    memset(result->bitmap, 0, result->num_bytes);

    //log_print("chains", "Chains before merge: %d", num_chains);

    // Todo: When we merge we don't set the direction, fix that?.
    for (int i = 0; i < num_chains; i++) {
        for (int j = 0; j < num_chains; j++) {
            if (j == i) continue;
            if (link_distance2(chains[i].head, chains[j].head) < CHAIN_MERGE_DISTANCE2) {

                Link *temp_head = chains[i].head;
                reverse_chain(&chains[i]);
                temp_head->next = chains[j].head;
                chains[j].head->previous = temp_head;
                chains[i].tail = chains[j].tail;
                chains[i].length += chains[j].length;

                num_chains--;
                chains[j] = chains[num_chains];

                i = 0;
                j = 0;

            } else if (link_distance2(chains[i].head, chains[j].tail) < CHAIN_MERGE_DISTANCE2) {

                chains[j].tail->next = chains[i].head;
                chains[i].head->previous = chains[j].tail;
                chains[j].tail = chains[i].tail;

                chains[j].length += chains[i].length;

                chains[i] = chains[j];

                num_chains--;
                chains[j] = chains[num_chains];

                i = 0;
                j = 0;

            } else if (link_distance2(chains[i].tail, chains[j].head) < CHAIN_MERGE_DISTANCE2) {

                chains[i].tail->next = chains[j].head;
                chains[j].head->previous = chains[i].tail;
                chains[i].tail = chains[j].tail;

                chains[i].length += chains[j].length;

                num_chains--;
                chains[j] = chains[num_chains];

                i = 0;
                j = 0;


            } else if (link_distance2(chains[i].tail, chains[j].tail) < CHAIN_MERGE_DISTANCE2) {

                Link *temp_head = chains[j].head;
                Link *temp_tail = chains[j].tail;
                reverse_chain(&chains[j]);
                chains[i].tail->next = temp_tail;
                temp_tail->previous = chains[i].tail;
                chains[i].tail = temp_head;

                chains[i].length += chains[j].length;

                num_chains--;
                chains[j] = chains[num_chains];

                i = 0;
                j = 0;
            }

        }
    }

    //log_print("chains", "Chains after merge: %d", num_chains);
    int num_actual_chains = 0;
    for (int i = 0; i < num_chains; i++) {
        if (chain_BB_surface(chains[i]) > MIN_CHAIN_BB_SURFACE) {
            Link *link = chains[i].head;
            while (true) {
                result->bitmap[(link->x + link->y * result->width) * 4] = 255;
                result->bitmap[(link->x + link->y * result->width) * 4 + 3] = 255;

                if (link == chains[i].head) {
                    result->bitmap[(link->x + link->y * result->width) * 4 + 1] = 255;
                }

                if (link == chains[i].tail) {
                    result->bitmap[(link->x + link->y * result->width) * 4 + 2] = 255;
                }

                if (link->next == NULL) break;

                link = link->next;
                free(link->previous);
            }

            free(link);
            num_actual_chains++;
        } else {

            Link *link = chains[i].head;
            while (link->next != NULL) {
                link = link->next;
                free(link->previous);
            }

            free(link);
        }
    }

    //log_print("chains", "Chains after filter: %d", num_actual_chains);
}

// Top: 0, Left: 1, Bottom: 2, Right: 3
int is_on_box_boundary(int x, int y, int *side) {
    for (int i = 0; i < num_boxes; i++) {

        if (x >= boxes[i].left && x <= boxes[i].right &&
            y == boxes[i].top) {
            if (side) *side = 0;
            return i;
        }

        if (x == boxes[i].left &&
            y >= boxes[i].top && y <= boxes[i].bottom) {
            if (side) *side = 1;
            return i;
        }

        if (x >= boxes[i].left && x <= boxes[i].right &&
            y == boxes[i].bottom) {
            if (side) *side = 2;
            return i;
        }

        if (x == boxes[i].right &&
            y >= boxes[i].top && y <= boxes[i].bottom) {
            if (side) *side = 3;
            return i;
        }
    }

    return -1;
}

void remove_boxes_content(Image *image, Image *result) {
    result->width = image->width;
    result->height = image->height;
    result->bytes_per_pixel = 4;
    result->bitmap = (unsigned char *) malloc(
            result->width * result->height * result->bytes_per_pixel);
    result->width_in_bytes = result->width * result->bytes_per_pixel;
    result->num_bytes = result->width * result->height * result->bytes_per_pixel;
    memset(result->bitmap, 0, result->num_bytes);
    for (int i = 0; i < image->height; i++) {
        for (int j = 0; j < image->width; j++) {
            unsigned char pixel_b = image->bitmap[(j + i * image->width) * 4];
            if (pixel_b == 0) {
                if (!inside_box(j, i) || (is_on_box_boundary(j, i, NULL) >= 0)) {
                    result->bitmap[(j + i * image->width) * 4] = 255;
                }
            }
        }
    }
}

struct v2 {
    int x, y;
};

void identity_connections(Image *image, int *matrix, int *matrix_id, int *matrix_sides) {
    Image temp;
    temp.width = image->width;
    temp.height = image->height;
    temp.bytes_per_pixel = 4;
    temp.bitmap = (unsigned char *) malloc(temp.width * temp.height * temp.bytes_per_pixel);
    temp.width_in_bytes = temp.width * temp.bytes_per_pixel;
    temp.num_bytes = temp.width * temp.height * temp.bytes_per_pixel;
    scope_exit(free(temp.bitmap));

    int lowest_pixel_y = -1;
    int lowest_pixel_x = -1;

    int center_x = image->width / 2;

    int num_connections_found = 0;
    ground_node = -1;

    // First pass: Follow wires
    for (int i = 0; i < image->height; i++) {
        for (int j = 0; j < image->width; j++) {
            unsigned char pixel_b = image->bitmap[(j + i * image->width) * 4];
            unsigned char temp_pixel_b = temp.bitmap[(j + i * image->width) * 4];
            if (pixel_b == 255 && temp_pixel_b != 255) {
                // Found a pixel, let's run a BFS on it.
                bool hit_a_box = false;
                int potential_ground_node = ground_node;
                int potential_lowest_pixel_y = lowest_pixel_y;
                int potential_lowest_pixel_x = lowest_pixel_x;
                int *boxes_hit = (int *) malloc(sizeof(int) * num_boxes);
                memset(boxes_hit, -1, num_boxes * sizeof(int));
                scope_exit(free(boxes_hit));
                std::queue<v2> q;
                v2 pix = {j, i};

                q.push(pix);

                while (!q.empty()) {
                    v2 current = q.front();
                    q.pop();
                    temp.bitmap[(current.x + current.y * image->width) * 4] = 255;

                    if (potential_lowest_pixel_y == -1 || potential_lowest_pixel_y <= i) {
                        potential_lowest_pixel_y = i;

                        if (potential_lowest_pixel_x == -1 ||
                            (potential_lowest_pixel_y > lowest_pixel_y) ||
                            (abs(potential_lowest_pixel_x - center_x) > abs(j - center_x))) {
                            potential_lowest_pixel_x = j;

                            potential_ground_node = num_connections_found + 1;
                        }
                    }


                    // Enqueue next pixels
                    if (current.x < image->width - 1) {
                        if (image->bitmap[(current.x + 1 + current.y * image->width) * 4] == 255 &&
                            temp.bitmap[(current.x + 1 + current.y * image->width) * 4] != 255) {
                            v2 new_v = {current.x + 1, current.y};
                            temp.bitmap[(current.x + 1 + current.y * image->width) * 4] = 255;
                            q.push(new_v);
                        }
                    }

                    if (current.x > 1) {
                        if (image->bitmap[(current.x - 1 + current.y * image->width) * 4] == 255 &&
                            temp.bitmap[(current.x - 1 + current.y * image->width) * 4] != 255) {
                            v2 new_v = {current.x - 1, current.y};
                            temp.bitmap[(current.x - 1 + current.y * image->width) * 4] = 255;
                            q.push(new_v);
                        }
                    }

                    if (current.y < image->height - 1) {
                        if (image->bitmap[(current.x + (current.y + 1) * image->width) * 4] ==
                            255 &&
                            temp.bitmap[(current.x + (current.y + 1) * image->width) * 4] != 255) {
                            v2 new_v = {current.x, current.y + 1};
                            temp.bitmap[(current.x + (current.y + 1) * image->width) * 4] = 255;
                            q.push(new_v);
                        }
                    }

                    if (current.y > 1) {
                        if (image->bitmap[(current.x + (current.y - 1) * image->width) * 4] ==
                            255 &&
                            temp.bitmap[(current.x + (current.y - 1) * image->width) * 4] != 255) {
                            v2 new_v = {current.x, current.y - 1};
                            temp.bitmap[(current.x + (current.y - 1) * image->width) * 4] = 255;
                            q.push(new_v);
                        }
                    }

                    // Check for box intersection
                    int side = -1;
                    int n = is_on_box_boundary(current.x, current.y, &side);
                    if (n >= 0) {
                        boxes_hit[n] = side;
                        hit_a_box = true;
                    }
                }

                bool should_increment = false;
                if (hit_a_box) {
                    for (int m_i = 0; m_i < num_boxes; m_i++) {
                        for (int m_j = 0; m_j < num_boxes; m_j++) {
                            if (m_i == m_j) {
                                if (boxes_hit[m_i] >= 0) {
                                    matrix[m_i + m_j * num_boxes]++;
                                }
                                continue;
                            }

                            if (boxes_hit[m_i] >= 0 && boxes_hit[m_j] >= 0) {
                                matrix_id[m_i + m_j * num_boxes] = num_connections_found + 1;

                                matrix_sides[m_i + m_j * num_boxes] = boxes_hit[m_i];

                                matrix[m_i + m_j * num_boxes]++;

                                should_increment = true;

                            }
                        }
                    }

                    if (should_increment) {
                        ground_node = potential_ground_node;
                        lowest_pixel_y = potential_lowest_pixel_y;
                        lowest_pixel_x = potential_lowest_pixel_x;
                        num_connections_found++;
                    }
                }
            }
        }
    }

    // Second (overwriting) pass: Look for overlapping boxes
    for (int i = 0; i < num_boxes; i++) {
        for (int j = i + 1; j < num_boxes; j++) {
            if (box_overlap(&boxes[i], &boxes[j]) > 0.0f) {
                matrix[i + j * num_boxes] = 1;
                matrix[j + i * num_boxes] = 1;
            }
        }
    }


    // Third pass: compact node ids
    for (int n = 1; n < num_connections_found; n++) {
        bool found = false;
        for (int i = 0; i < num_boxes; i++) {
            for (int j = 0; j < num_boxes; j++) {
                if(i == j) continue;
                if (matrix_id[i + j * num_boxes] == n) {
                    found = true;
                }
            }
        }

        if(!found) {
            if (ground_node == n) {
                ground_node--;
            }

            for (int i = 0; i < num_boxes; i++) {
                for (int j = 0; j < num_boxes; j++) {
                    if(i == j) continue;
                    if(matrix_id[i + j * num_boxes] >= n) {
                        matrix_id[i + j * num_boxes]--;
                    }
                }
            }

            num_connections_found--;
            n = 0;
        }
    }

}

bool do_the_generator_fix(int image_height, int image_width) {
    for (int i = 0; i < num_boxes; i++) {
        for (int j = i + 1; j < num_boxes; j++) {
            if (connection_matrix[i + j * num_boxes] == 2) { // 2 connections betweeen boxes i and j
                // We assume that's a generator whose boxes got split up, so we'll merge them and expand the box

                boxes[i].left = min(boxes[i].left, boxes[j].left);
                boxes[i].right = max(boxes[i].right, boxes[j].right);
                boxes[i].top = min(boxes[i].top, boxes[j].top);
                boxes[i].bottom = max(boxes[i].bottom, boxes[j].bottom);

                // Let's figure out if it's horizontal or vertical
                int width = boxes[i].right - boxes[i].left;
                int height = boxes[i].bottom - boxes[i].top;

                if (width > height) {
                    // Expand height
                    height = 0.8 * width;
                    int mid = (boxes[i].top + boxes[i].bottom) * 0.5;
                    boxes[i].top = max(mid - height / 2, 0);
                    boxes[i].bottom = min(image_height, mid + height / 2);


                } else {
                    // Expand width
                    width = 0.8 * height;
                    int mid = (boxes[i].left + boxes[i].right) * 0.5;
                    boxes[i].left = max(mid - width / 2, 0);
                    boxes[i].right = min(image_width, mid + width / 2);
                }

                num_boxes--;
                boxes[j] = boxes[num_boxes];

                return true;
            }
        }
    }

    return false;
}

// Noise in the picture is being detected as labels, so we'll remove label boxes that don't match a minimum pixel weight
void filter_labels() {
    for (int i = num_connections; i < num_boxes; i++) {
        float weight = 0.0f;

        for (int x = boxes[i].left; x <= boxes[i].right; x++) {
            for (int y = boxes[i].top; y <= boxes[i].bottom; y++) {
                if (labels_test_image.bitmap[(x + y * labels_test_image.width) * 4] == 0) {
                    weight += 1;
                }
            }
        }

        weight /= (boxes[i].right - boxes[i].left + 1) * (boxes[i].bottom - boxes[i].top + 1);

        if (weight < 0.05f) {
            num_boxes--;
            boxes[i] = boxes[num_boxes];
            i--;
        }
    }
}


Image do_bw_conversion(Image image, float threshold) {
    //rgb_to_bgr(&image);

    convert_to_bw(&image, &bw_test_image, threshold);

    return bw_test_image;
}

ProcessingResult do_image_processing(Image test_image) {
    ProcessingResult pr;

    result_test_image.width = test_image.width;
    result_test_image.height = test_image.height;
    result_test_image.bytes_per_pixel = 4;
    result_test_image.bitmap = (unsigned char *) malloc(
            test_image.width * test_image.height * test_image.bytes_per_pixel);
    result_test_image.width_in_bytes = test_image.width * test_image.bytes_per_pixel;
    result_test_image.num_bytes = test_image.width * test_image.height * test_image.bytes_per_pixel;

    memcpy(result_test_image.bitmap, test_image.bitmap, result_test_image.num_bytes);

    find_chains(&test_image, &chains_test_image);

    do_the_thing(&chains_test_image, &components_test_image, 6, 0);

    do_the_thing(&components_test_image, &dtt_test_image, 30, 175);

    // Filter text
    labels_test_image.width = components_test_image.width;
    labels_test_image.height = components_test_image.height;
    labels_test_image.bytes_per_pixel = 4;
    labels_test_image.bitmap = (unsigned char *) malloc(
            components_test_image.width * components_test_image.height *
            components_test_image.bytes_per_pixel);
    labels_test_image.width_in_bytes =
            components_test_image.width * components_test_image.bytes_per_pixel;
    labels_test_image.num_bytes = components_test_image.width * components_test_image.height *
                                  components_test_image.bytes_per_pixel;

    // Separation of text and components
    for (int x = 0; x < components_test_image.width; x++) {
        for (int y = 0; y < components_test_image.height; y++) {
            if (components_test_image.bitmap[(x + y * components_test_image.width) * 4] == 255) {
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 +
                                             0] = test_image.bitmap[
                        (x + y * components_test_image.width) * 4 + 0];
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 +
                                             1] = test_image.bitmap[
                        (x + y * components_test_image.width) * 4 + 1];
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 +
                                             2] = test_image.bitmap[
                        (x + y * components_test_image.width) * 4 + 2];
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 +
                                             3] = test_image.bitmap[
                        (x + y * components_test_image.width) * 4 + 3];

                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 0] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 1] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 2] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 3] = 255;

            } else {
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 + 0] = 255;
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 + 1] = 255;
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 + 2] = 255;
                components_test_image.bitmap[(x + y * components_test_image.width) * 4 + 3] = 255;

                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 0] =
                        255 - test_image.bitmap[(x + y * labels_test_image.width) * 4 + 0];
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 1] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 2] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 3] = 255;
            }
        }
    }

    do_the_thing(&labels_test_image, &dtt_labels_test_image, 12, 0);

    // Conversion from blue to BW
    for (int x = 0; x < labels_test_image.width; x++) {
        for (int y = 0; y < labels_test_image.height; y++) {
            if (labels_test_image.bitmap[(x + y * labels_test_image.width) * 4] == 255) {
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 0] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 1] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 2] = 0;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 3] = 255;
            } else {
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 0] = 255;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 1] = 255;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 2] = 255;
                labels_test_image.bitmap[(x + y * labels_test_image.width) * 4 + 3] = 255;
            }
        }
    }

    // Component recognition
    find_boxes(&dtt_test_image, &dtot_components_test_image, &components_test_image);

    do {
        remove_boxes_content(&components_test_image, &connections_test_image);

        if (connection_matrix) free(connection_matrix);
        if (connection_matrix_with_id) free(connection_matrix_with_id);
        if (connection_matrix_with_sides) free(connection_matrix_with_sides);

        num_connections = num_boxes;
        connection_matrix = (int *) calloc(sizeof(int), num_connections * num_connections);
        connection_matrix_with_id = (int *) calloc(sizeof(int), num_connections * num_connections);
        connection_matrix_with_sides = (int *) calloc(sizeof(int),
                                                      num_connections * num_connections);

        identity_connections(&connections_test_image, connection_matrix, connection_matrix_with_id,
                             connection_matrix_with_sides);

    } while (do_the_generator_fix(test_image.height, test_image.width));


    bool need_regen_connections = false;
    for (int i = 0; i < num_boxes; i++) {
        if (box_size(&boxes[i]) < MIN_BOX_SIZE) {
            num_boxes--;
            boxes[i] = boxes[num_boxes];
            i--;
            need_regen_connections = true;
        }
    }

    if (need_regen_connections) {
        remove_boxes_content(&components_test_image, &connections_test_image);

        if (connection_matrix) free(connection_matrix);
        if (connection_matrix_with_id) free(connection_matrix_with_id);
        if (connection_matrix_with_sides) free(connection_matrix_with_sides);

        num_connections = num_boxes;
        connection_matrix = (int *) calloc(sizeof(int), num_connections * num_connections);
        connection_matrix_with_id = (int *) calloc(sizeof(int), num_connections * num_connections);
        connection_matrix_with_sides = (int *) calloc(sizeof(int),
                                                      num_connections * num_connections);

        identity_connections(&connections_test_image, connection_matrix, connection_matrix_with_id,
                             connection_matrix_with_sides);
    }

    draw_boxes(&dtot_components_test_image, 0, num_boxes);

    // Label recognition
    find_boxes(&dtt_labels_test_image, &dtot_labels_test_image, &labels_test_image);

    filter_labels();

    for (int i = num_connections; i < num_boxes; i++) {
        boxes[i].is_label = true;
    }

    // If a component box has no connections, we'll assume it's a label
    for (int i = 0; i < num_connections; i++) {
        bool has_connection = false;

        for (int m_j = 0; m_j < num_connections; m_j++) {
            if (i == m_j) continue;
            if (connection_matrix[i * num_connections + m_j] == 1) {
                has_connection = true;
                break;
            }
        }

        if (!has_connection) {
            boxes[i].is_label = true;
        }
    }

    draw_boxes(&dtot_labels_test_image, num_connections, num_boxes);

    draw_boxes(&result_test_image, 0, num_boxes);

    pr.num_connections = num_connections;
    pr.connections = (int *) malloc(num_connections * sizeof(int));
    memcpy(pr.connections, connection_matrix, num_connections * sizeof(int));

    pr.connections_with_id = (int *) malloc(num_connections * sizeof(int));
    memcpy(pr.connections_with_id, connection_matrix_with_id, num_connections * sizeof(int));

    pr.connections_with_sides = (int *) malloc(num_connections * sizeof(int));
    memcpy(pr.connections_with_sides, connection_matrix_with_sides, num_connections * sizeof(int));

    pr.ground_node = ground_node;

    pr.num_boxes = num_boxes;
    pr.boxes = (Box *) malloc(num_boxes * sizeof(Box));
    memcpy(pr.boxes, boxes, num_boxes * sizeof(Box));
    return pr;
}
