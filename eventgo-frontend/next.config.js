/** @type {import('next').NextConfig} */
const nextConfig = {
	images: {
		remotePatterns: [
			{
				protocol: "https",
				hostname: "media.gettyimages.com",
			},
			{
				protocol: "https",
				hostname: "media-cldnry.s-nbcnews.com",
			},
			{
				protocol: "https",
				hostname: "static.wikia.nocookie.net",
			},
			{
				protocol: "https",
				hostname: "media1.s-nbcnews.com",
			},
			{
				protocol: "https",
				hostname: "tmw.com.sg",
			},
			{
				protocol: "https",
				hostname: "talkstar-photos.s3.amazonaws.com",
			},
			{
				protocol: "https",
				hostname: "www.pelago.com",
			},
			{
				protocol: "https",
				hostname: "encrypted-tbn0.gstatic.com",
			},
			{
				protocol: "https",
				hostname: "images.ra.co",
			},
			{
				protocol: "https",
				hostname: "singaporegp.sg",
			},
		],
	},
};

module.exports = nextConfig;
