import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Seeding database...');

  // Create demo users with different languages
  const users = [
    {
      email: 'alice@example.com',
      username: 'alice',
      password: 'password123',
      preferredLanguage: 'en',
    },
    {
      email: 'carlos@example.com',
      username: 'carlos',
      password: 'password123',
      preferredLanguage: 'es',
    },
    {
      email: 'marie@example.com',
      username: 'marie',
      password: 'password123',
      preferredLanguage: 'fr',
    },
    {
      email: 'yuki@example.com',
      username: 'yuki',
      password: 'password123',
      preferredLanguage: 'ja',
    },
  ];

  const createdUsers = [];

  for (const user of users) {
    const passwordHash = await bcrypt.hash(user.password, 10);
    const created = await prisma.user.upsert({
      where: { email: user.email },
      update: {},
      create: {
        email: user.email,
        username: user.username,
        passwordHash,
        preferredLanguage: user.preferredLanguage,
      },
    });
    createdUsers.push(created);
    console.log(`✅ Created user: ${created.username} (${created.preferredLanguage})`);
  }

  // Create a conversation between Alice and Carlos
  const conversation = await prisma.conversation.create({
    data: {
      type: 'DIRECT',
      members: {
        create: [
          { userId: createdUsers[0].id },
          { userId: createdUsers[1].id },
        ],
      },
    },
  });

  console.log(`✅ Created conversation: ${conversation.id}`);

  // Add some sample messages
  const messages = [
    {
      senderId: createdUsers[0].id,
      originalContent: 'Hello Carlos! How are you?',
      originalLanguage: 'en',
    },
    {
      senderId: createdUsers[1].id,
      originalContent: '¡Hola Alice! Estoy muy bien, gracias. ¿Y tú?',
      originalLanguage: 'es',
    },
    {
      senderId: createdUsers[0].id,
      originalContent: "I'm doing great! I'm excited to test this translation app.",
      originalLanguage: 'en',
    },
  ];

  for (const msg of messages) {
    const message = await prisma.message.create({
      data: {
        conversationId: conversation.id,
        senderId: msg.senderId,
        originalContent: msg.originalContent,
        originalLanguage: msg.originalLanguage,
        type: 'TEXT',
        status: 'SENT',
      },
    });
    console.log(`✅ Created message: ${message.id.slice(0, 8)}...`);
  }

  // Create a group conversation
  const groupConversation = await prisma.conversation.create({
    data: {
      type: 'GROUP',
      name: 'International Friends',
      members: {
        create: createdUsers.map((u) => ({ userId: u.id })),
      },
    },
  });

  console.log(`✅ Created group conversation: ${groupConversation.name}`);

  console.log('\n🎉 Seeding completed!');
}

main()
  .catch((e) => {
    console.error('❌ Seeding failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });



