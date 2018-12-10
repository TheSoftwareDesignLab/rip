# coding=utf-8
import cv2
import sys
import dhash
import json
from pprint import pprint
from PIL import Image
from skimage.measure import compare_ssim

RIP_FILE_PATH = "demoProject/tree.json"

def loadRIPGraph(filePath):
    with open(filePath) as file:
        data = json.load(file)
        nodes = data['nodes']
        for node in nodes:
            if(node['imageName'] != 'NA'):
                print(node['imageName'])

def same_ext(img_strc, img_video):
    if img_strc.split(".")[-1]!= img_video.split(".")[-1]:
        raise Exception("Las dos imagenes no tienen la misma extensión")

def min_size(img1,img2):
    height, width= img1.shape
    height2, width2 = img2.shape
    return (min(height,height2),min(width,width2))


def max_size(img1,img2):
    height, width= img1.shape
    height2, width2 = img2.shape
    return (max(height,height2),max(width,width2))

def grayImages(img1,img2):
     image1 = cv2.cvtColor(img1, cv2.COLOR_BGR2GRAY) # grayed  image
     image2 = cv2.cvtColor(img2, cv2.COLOR_BGR2GRAY) # grayed  image
     return image1,image2

def dHash_hammingDistance(img1,img2):
    hImg1 = dhash.dhash_int(img1) # calculate jash for the image
    hImg2 = dhash.dhash_int(img2) # calculate jash for the image
    return dhash.get_num_bits_different(hImg1,hImg2) # hamming distance between images

def read_images(pimg1,pimg2):
    #read the images  (Images in black and white) and PIL Objects
    img1 = cv2.imread(pimg1,0) 
    img2 = cv2.imread(pimg2,0)
    # Pil Objects for dash and hamming distance
    image = Image.open(pimg1)
    image2 = Image.open(pimg2)
    return(img1,img2,image,image2)
    
 
def readImages(pimg1,pimg2,method = 'both'):

    #reading the paths --> images and pil  objects
    img1,img2,image,image2 = read_images(pimg1,pimg2)
    
    score = 0 
    heightx =-1
    height =-1
    width =-1
    widthx =-1
    
    #Find max and min height and width
    if method == 'zooming':
        heightx, widthx = max_size(img1,img2) # max size between a pair of images
    elif method == 'shrinking': #Decimation / shrinking 
        height, width = min_size(img1,img2) # min size between a pair of images
    else:
        heightx, widthx = max_size(img1,img2)
        height, width = min_size(img1,img2) # min size between a pair of images

    
    rximg1 = cv2.resize(img1,(widthx,heightx),interpolation =cv2.INTER_NEAREST )
    rximg2 = cv2.resize(img2,(widthx,heightx),interpolation =cv2.INTER_NEAREST )
    rimg1 = cv2.resize(img1,(width,height),interpolation =cv2.INTER_NEAREST)
    rimg2 = cv2.resize(img2,(width,height),interpolation =cv2.INTER_NEAREST)

    #clahe = cv2.createCLAHE(clipLimit=1.0, tileGridSize=(3,3))
    #rximg1 = clahe.apply(rximg1)     
    #rximg2 = clahe.apply(rximg2)

    #Compare
    hammingDistance = dHash_hammingDistance(image,image2)
    score = compare_ssim(rimg1,rimg2, full=True)
    score2 = compare_ssim(rximg1,rximg2, full=True)

    return score,score2,hammingDistance 
    

def main(argvs):
    score,score2,hammingDistance  = readImages(sys.argv[1],sys.argv[2])
    print (argvs)
    if len(argvs) > 3:
        f = open(sys.argv[3], "a")
        f.write(str(score[0])+"\n")
        f.write(str(score2[0])+"\n")
        f.write(str(hammingDistance)+"\n")
        f.close()
    
    print (score[0])
    print(score2[0])
    print(hammingDistance)

    loadRIPGraph(RIP_FILE_PATH)


if __name__== "__main__":
    main(sys.argv)
