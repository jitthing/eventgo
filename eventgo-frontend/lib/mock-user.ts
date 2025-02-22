export const mockUser = {
    id: 1,
    name: "John Doe",
    email: "johndoe@example.com",
    avatar: "https://api.dicebear.com/6.x/initials/svg?seed=John+Doe",
  };
  
  export async function getUserProfile() {
    return new Promise((resolve) => {
      setTimeout(() => resolve(mockUser), 500); // Simulate API delay
    });
  }
  