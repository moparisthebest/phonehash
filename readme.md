Quickly reverse Kontalk sha1 hashes to phone numbers
--------------------------------------------

[Kontalk](https://kontalk.net/) JIDs are simply [sha1 hashes of phone numbers](https://github.com/kontalk/androidclient/issues/497) in the form of beb578a7ae7e6b93b49a619e6709a1c3b1063e9c@kontalk.net, which was generated from the phone number +15555555555

They already know this isn't much protection, but I wanted to see how fast I could go from hash to phone number, and it turned out to be harder than I thought.  I'll try to walk you through my thought process here.

tl;dr I put all 11 digit phone numbers represented as 5 byte integers in a 500gb file sorted by their sha1 hashes, now I can binary search it fast.

We are talking about 100 billion possible numbers here, 0 - 99,999,999,999.  The smallest number of bytes that number can be represented with is 5, and a sha1 hash is 20 bytes.  So if you wanted to generate and store the entire list of sha1 hashes and phone numbers, you'd need 100,000,000,000 * 25 bytes of space, or 2.5 TB.  I don't care to waste that much space on this, so I decided to store only the phone numbers (500 GB), but sorted by the sha1 hash, by generating the sha1 hash to sort, and only writing phone numbers to disk.  The good news is that, if this list is sorted, a [binary search](https://en.wikipedia.org/wiki/Binary_search_algorithm) only costs O(log n) in the worst case, so that's worst case computing 26 sha1 hashes on each search, which will be plenty fast on even the most modest of hardware.

Interestingly as a side-note this is the first time I have been bitten by java only supporting 32-bit signed integers as array indices, as this requires a much bigger array.  I ended up writing a [List implementation backed by a RandomAccessFile](https://github.com/moparisthebest/filelists) just for this very purpose.

Sorting turned out to be a challenge itself, since I'm not storing the sha1 hash, I have to generate it for each compare, and you start to care about the number of compares in your sort algorithm.  Also, the best ones (merge/quicksort) require O(2n) space, and even O(1.5n) space is too much in this case.  Also I couldn't use any of the built-in java ones because they only operate on arrays with 32-bit indices.  I wrote a few implementations for RandomAccessFileList, and the fastest was heap sort, but the time it took to sort 1 million rows extrapolated out to 100 billion was looking to be somewhere around 4 years of constant runtime.  Finally I had an epiphany, essentially do a bucket sort but during generation so it doesn't take 2n space.  I have multiple threads iterate over portions of the range, generate sha1 hashes for each number, and write them to files sorted out on the first 2 bytes of the sha1 hash.  This gives you 65535 files about 7.3 MB each, then you just need to sort them and concatenate them together and done.  This requires only 500 GB + 7.3 MB of space for a final result of a perfectly sorted 500 GB file.

The generation and sorting to 65535 buckets phase took about 32 hours with 4 threads and slow spinning drives. The sorting each and concatenating phase I limited to one thread so as not to kill my slow drives with seek times, and it took about 57 hours to finish.  If you had an SSD to do this on and didn't have to worry about seek times this would all be significantly faster, the speed was almost entirely constrained by disk performance.

The end result was worth it though, you can now go to [https://www.moparisthebest.com/phonehash/](https://www.moparisthebest.com/phonehash/) to reverse a Kontalk sha1 hash to a phone number in around 2 seconds with essentially no load on my server.  I also included all the tools to generate and use this yourself in this repo.  PhoneBucketGen will generate this massive sorted phone number file, and WebApp will run a single web service to return answers for you, I also included the html/javascript to run the website.

Enjoy!
