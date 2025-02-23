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
		],
	},
};

module.exports = nextConfig;
