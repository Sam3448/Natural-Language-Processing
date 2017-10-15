# Natural-Language-Processing
Coded by Weicheng Zhang.

## Introduction
This is the course project for Natural Language Processing, Fall 2017.

Homework 1: using grammar file and sentence generator to generate sentences with complex structure.
Homework 2: using Word2Vec and cosine similarity to get similar neighbors of one word or a group of words.

## Instruction & Parameters

1. Homework 1: the q1.jar can be used to generate sentences based on certain grammar files. 
  	It takes three parameters: 
  	* t/f: t lets the generator randomly generating sentences and shown in dependency tree structure. f lets the generator randomly generating sentences and simply shows the sentence with no structure information.
	* file path: the path of the grammar file.
	* num: the number of sentences that need to be generated.

2. Homework 2: the q8.jar can be used to determine the 10 most similar words to one or several given words. Also, we can use ./findsim "FilePath" word1/word1+word2+word3 to generate similar words.
	It takes two parameters:
	* file path: the path of the grammar file.
	* word/words: one word or three words as the original words for finding neighbors

3. Homework 3: the ./textcat can be used to generate training models based on AddLambda, BackoffAddLambda, Loglinear models.
