-- Seed data for IELTS topics
USE ielts_data;

-- Writing Topics (Task 1)
INSERT INTO writing_topics (task_type, title, chart_type, chart_description, category, difficulty) VALUES
(1, 'The chart below shows the percentage of households in owned and rented accommodation in England and Wales between 1918 and 2011.', 'bar', 'A bar chart comparing owned vs rented accommodation percentages across decades from 1918 to 2011.', 'housing', 3),
(1, 'The graph below shows the quantities of goods transported in the UK between 1974 and 2002 by four different modes of transport.', 'line', 'A line graph showing goods transported by road, water, rail, and pipeline from 1974 to 2002 in million tonnes.', 'transport', 3),
(1, 'The pie charts below show the comparison of different kinds of energy production of France in two years.', 'pie', 'Two pie charts comparing energy production sources (nuclear, thermal, hydroelectric, other) in 1995 and 2005.', 'energy', 2),
(1, 'The table below gives information about the underground railway systems in six cities.', 'table', 'A table showing date opened, kilometres of route, and passengers per year for London, Paris, Tokyo, Washington DC, Kyoto, and Los Angeles metro systems.', 'infrastructure', 2),
(1, 'The maps below show the centre of a small town called Islip as it is now, and plans for its development.', 'map', 'Two maps showing the current layout of Islip town centre and proposed redevelopment plans including new housing, shops, and a dual carriageway.', 'urban_planning', 4);

-- Writing Topics (Task 2)
INSERT INTO writing_topics (task_type, title, chart_type, chart_description, category, difficulty) VALUES
(2, 'Some people think that the best way to reduce crime is to give longer prison sentences. Others, however, believe there are better alternative ways of reducing crime. Discuss both views and give your opinion.', NULL, NULL, 'crime', 3),
(2, 'In many countries, the amount of crime is increasing. What do you think are the main causes of crime? What can be done to deal with this problem?', NULL, NULL, 'crime', 3),
(2, 'Some people believe that unpaid community service should be a compulsory part of high school programmes. To what extent do you agree or disagree?', NULL, NULL, 'education', 3),
(2, 'Universities should accept equal numbers of male and female students in every subject. To what extent do you agree or disagree?', NULL, NULL, 'education', 3),
(2, 'Some people say that the main environmental problem of our time is the loss of particular species of plants and animals. Others say that there are more important environmental problems. Discuss both these views and give your own opinion.', NULL, NULL, 'environment', 4),
(2, 'The increase in the production of consumer goods results in damage to the natural environment. What are the causes of this? What can be done to solve this problem?', NULL, NULL, 'environment', 3),
(2, 'Some people think that all university students should study whatever they like. Others believe that they should only be allowed to study subjects that will be useful in the future, such as those related to science and technology. Discuss both views and give your opinion.', NULL, NULL, 'education', 3),
(2, 'In some countries, an increasing number of people are suffering from health problems as a result of eating too much fast food. It is therefore necessary for governments to impose a higher tax on this kind of food. To what extent do you agree or disagree?', NULL, NULL, 'health', 3),
(2, 'Some people believe that it is best to accept a bad situation, such as an unsatisfactory job or shortage of money. Others argue that it is better to try and improve such situations. Discuss both views and give your own opinion.', NULL, NULL, 'society', 3),
(2, 'Many governments think that economic progress is their most important goal. Some people, however, think that other types of progress are equally important for a country. Discuss both views and give your own opinion.', NULL, NULL, 'economy', 4);

-- Speaking Topics (Part 1)
INSERT INTO speaking_topics (part, question, cue_card, follow_up_questions, category, season) VALUES
(1, 'Do you work or are you a student?', NULL, NULL, 'work_study', '2024S1'),
(1, 'What do you like about your job/studies?', NULL, NULL, 'work_study', '2024S1'),
(1, 'Do you live in a house or an apartment?', NULL, NULL, 'accommodation', '2024S1'),
(1, 'What is your favourite room in your home?', NULL, NULL, 'accommodation', '2024S1'),
(1, 'How often do you use the internet?', NULL, NULL, 'technology', '2024S1'),
(1, 'What do you usually do on the internet?', NULL, NULL, 'technology', '2024S1'),
(1, 'Do you like reading books? What kind of books do you like?', NULL, NULL, 'hobbies', '2024S1'),
(1, 'How do you usually spend your weekends?', NULL, NULL, 'leisure', '2024S1'),
(1, 'Do you prefer to travel alone or with friends?', NULL, NULL, 'travel', '2024S1'),
(1, 'What kind of weather do you like?', NULL, NULL, 'weather', '2024S1');

-- Speaking Topics (Part 2)
INSERT INTO speaking_topics (part, question, cue_card, follow_up_questions, category, season) VALUES
(2, 'Describe a book that you have read recently.',
 'You should say:\n- what the book was about\n- why you decided to read it\n- what you liked or disliked about it\nand explain whether you would recommend it to others.',
 '["Do you often read books?", "What types of books are popular in your country?", "Do you think reading is important? Why?"]',
 'hobbies', '2024S1'),
(2, 'Describe a place you have visited that you particularly liked.',
 'You should say:\n- where it was\n- when you went there\n- what you did there\nand explain why you liked it so much.',
 '["Do you like travelling?", "What kind of places do you prefer to visit?", "Do you think tourism is good for local communities?"]',
 'travel', '2024S1'),
(2, 'Describe a person who has had a great influence on you.',
 'You should say:\n- who this person is\n- how you know this person\n- what this person has done to influence you\nand explain why they have had such a great influence.',
 '["Who influences young people more, parents or friends?", "Do you think famous people are good role models?", "Has the influence of teachers changed over time?"]',
 'people', '2024S1'),
(2, 'Describe a skill that took you a long time to learn.',
 'You should say:\n- what the skill was\n- when you started learning it\n- why it took so long to learn\nand explain how you felt when you finally mastered it.',
 '["What skills are important for young people to learn?", "Do you think schools teach enough practical skills?", "Is it better to learn skills online or in person?"]',
 'education', '2024S1'),
(2, 'Describe a time when you helped someone.',
 'You should say:\n- who you helped\n- how you helped them\n- why you helped them\nand explain how you felt about helping them.',
 '["Do you think people help each other more now than in the past?", "Why do some people volunteer?", "Should schools teach children to help others?"]',
 'society', '2024S1');

-- Speaking Topics (Part 3)
INSERT INTO speaking_topics (part, question, cue_card, follow_up_questions, category, season) VALUES
(3, 'What are the advantages and disadvantages of reading e-books compared to paper books?', NULL,
 '["Do you think paper books will disappear in the future?", "How has technology changed the way people read?", "Should governments do more to encourage reading?"]',
 'hobbies', '2024S1'),
(3, 'How has tourism changed in your country over the past few decades?', NULL,
 '["What are the negative effects of tourism on the environment?", "Should there be limits on the number of tourists visiting certain places?", "How can tourism be made more sustainable?"]',
 'travel', '2024S1'),
(3, 'Do you think the education system in your country prepares students well for work?', NULL,
 '["What changes would you make to the education system?", "Is university education necessary for success?", "How will education change in the next 20 years?"]',
 'education', '2024S1'),
(3, 'Some people say that we should spend money on exploring outer space. Others disagree. What is your opinion?', NULL,
 '["What are the practical benefits of space exploration?", "Should space exploration be funded by governments or private companies?", "Do you think humans will live on other planets in the future?"]',
 'science', '2024S1'),
(3, 'In what ways can governments encourage people to live healthier lifestyles?', NULL,
 '["Should unhealthy food be taxed more heavily?", "Is it the responsibility of individuals or governments to ensure people are healthy?", "How has the concept of a healthy lifestyle changed over time?"]',
 'health', '2024S1');
